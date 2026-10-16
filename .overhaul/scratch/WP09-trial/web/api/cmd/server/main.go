// Command server is the generator's HTTP API.
//
//	go run ./cmd/server             serve
//	go run ./cmd/server -selftest   generate one project per enabled target, then exit
//
// Reads its settings from the environment (see internal/config), refuses to start if any of them
// are contradictory, and shuts down on SIGTERM after letting in-flight downloads finish.
package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/base-android/generator-api/internal/config"
	"github.com/base-android/generator-api/internal/generate"
	"github.com/base-android/generator-api/internal/httpapi"
	"github.com/base-android/generator-api/internal/store"
)

func main() {
	selftest := flag.Bool("selftest", false, "generate one project per enabled target and exit")
	flag.Parse()

	log := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	slog.SetDefault(log)

	run := serve
	if *selftest {
		run = selfTest
	}
	if err := run(log); err != nil {
		log.Error("fatal", "error", err)
		os.Exit(1)
	}
}

// generators loads the catalogue and makes a generator for every enabled target it lists. A target
// that is enabled but missing from the catalogue (its template not merged yet, say) is reported and
// skipped rather than failing the boot.
func generators(ctx, stopping context.Context, cfg config.Config, opts generate.Options, log *slog.Logger) (
	*generate.Catalogue, map[string]*generate.Generator, error,
) {
	catalogue, err := generate.LoadCatalogue(ctx, opts)
	if err != nil {
		return nil, nil, err
	}
	enabled := map[string]*generate.Generator{}
	for _, key := range cfg.EnableTargets {
		target := catalogue.Target(key)
		if target == nil {
			log.Warn("ENABLE_TARGETS names a target the catalogue does not list", "target", key)
			continue
		}
		g, err := generate.New(stopping, opts, *target, catalogue.Rules)
		if err != nil {
			return nil, nil, err
		}
		enabled[key] = g
	}
	if len(enabled) == 0 {
		return nil, nil, fmt.Errorf("none of ENABLE_TARGETS %v is in the catalogue", cfg.EnableTargets)
	}
	return catalogue, enabled, nil
}

func options(cfg config.Config, workRoot string) generate.Options {
	return generate.Options{
		PythonBin:     cfg.PythonBin,
		GeneratorDir:  cfg.GeneratorDir,
		WorkRoot:      workRoot,
		Timeout:       cfg.GenerateTimeout,
		Concurrency:   cfg.MaxConcurrentGenerations,
		CatalogueFile: cfg.CatalogueFile,
	}
}

func serve(log *slog.Logger) error {
	cfg, err := config.Load()
	if err != nil {
		return err
	}
	log.Info("configuration", "summary", cfg.String())

	// Every generation works in a directory under this one, removed at exit, so nothing a generator
	// wrote outlives the process even if a request's own cleanup never ran.
	workRoot, err := os.MkdirTemp("", "generator-api-")
	if err != nil {
		return fmt.Errorf("creating the work directory: %w", err)
	}
	defer func() { _ = os.RemoveAll(workRoot) }()

	stopping, stop := context.WithCancel(context.Background())
	defer stop()

	bootCtx, cancelBoot := context.WithTimeout(context.Background(), time.Minute)
	defer cancelBoot()
	catalogue, enabled, err := generators(bootCtx, stopping, cfg, options(cfg, workRoot), log)
	if err != nil {
		return err
	}

	var st *store.Store
	var writer *store.Writer
	if cfg.Persistent() {
		st, err = store.New(cfg.DatabaseURL, cfg.IPSalt, log)
		if err != nil {
			return err
		}
		defer st.Close()
		go st.Connect(stopping)
		go st.RunRetention(stopping, store.Retention{
			Requests: cfg.RetainRequests, Events: cfg.RetainEvents, Names: cfg.RetainNames,
		})
		writer = store.NewWriter(st, 1024, log)
	} else {
		log.Warn("DATABASE_URL is unset: nothing will be recorded and the admin portal is off")
	}

	// A nil *Writer in the interface would not compare equal to nil.
	var sink httpapi.Sink
	if writer != nil {
		sink = writer
	}
	server := httpapi.New(cfg, catalogue, enabled, st, sink, log)

	httpServer := &http.Server{
		Addr:    cfg.Addr,
		Handler: server.Routes(),
		// Generous, because the slow request here is a download over a bad connection rather
		// than a slow handler. ReadHeaderTimeout is the one that actually protects anything.
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      5 * time.Minute,
		IdleTimeout:       2 * time.Minute,
	}

	signals := make(chan os.Signal, 1)
	signal.Notify(signals, os.Interrupt, syscall.SIGTERM)

	serverErr := make(chan error, 1)
	go func() {
		log.Info("listening", "addr", cfg.Addr, "targets", cfg.EnableTargets)
		if err := httpServer.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			serverErr <- err
		}
	}()

	select {
	case err := <-serverErr:
		return err
	case <-signals:
		log.Info("shutting down")
	}

	// Long enough for a download in progress to finish. Generations still running after it are
	// killed by stop(), so their children do not outlive the server.
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	shutdownErr := httpServer.Shutdown(shutdownCtx)
	stop()
	if writer != nil {
		drainCtx, cancelDrain := context.WithTimeout(context.Background(), 5*time.Second)
		writer.Close(drainCtx)
		cancelDrain()
	}
	if errors.Is(shutdownErr, context.DeadlineExceeded) {
		log.Warn("shutdown timed out with requests still open")
		return nil
	}
	return shutdownErr
}
