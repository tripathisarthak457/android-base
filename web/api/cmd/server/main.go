// Command server is the generator's HTTP API.
//
//	go run ./cmd/server
//
// Reads its settings from the environment (see internal/config), refuses to start if any of them
// are contradictory, and shuts down on SIGTERM after letting in-flight downloads finish.
package main

import (
	"context"
	"errors"
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
	log := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	slog.SetDefault(log)

	if err := run(log); err != nil {
		log.Error("fatal", "error", err)
		os.Exit(1)
	}
}

func run(log *slog.Logger) error {
	cfg, err := config.Load()
	if err != nil {
		return err
	}
	log.Info("configuration", "summary", cfg.String())

	generator, err := generate.New(
		cfg.PythonBin, cfg.GeneratorDir, cfg.GenerateTimeout, cfg.MaxConcurrentGenerations,
	)
	if err != nil {
		return err
	}

	// Boot context, separate from the server's lifetime: a database that is slow to come up
	// should delay startup, not be waited on forever.
	bootCtx, cancelBoot := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancelBoot()

	var st *store.Store
	if cfg.Persistent() {
		st, err = store.Open(bootCtx, cfg.DatabaseURL, cfg.IPSalt, log)
		if err != nil {
			return err
		}
		defer st.Close()
		log.Info("persistence enabled")
	} else {
		log.Warn("DATABASE_URL is unset: nothing will be recorded and the admin portal is off")
	}

	server, err := httpapi.New(bootCtx, cfg, generator, st, log)
	if err != nil {
		return err
	}

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

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)

	serverErr := make(chan error, 1)
	go func() {
		log.Info("listening", "addr", cfg.Addr)
		if err := httpServer.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			serverErr <- err
		}
	}()

	select {
	case err := <-serverErr:
		return err
	case <-stop:
		log.Info("shutting down")
	}

	// Long enough for a download in progress to finish; the deploy script waits for it.
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	return httpServer.Shutdown(shutdownCtx)
}
