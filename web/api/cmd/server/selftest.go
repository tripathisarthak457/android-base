package main

import (
	"archive/zip"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"os"
	"time"

	"github.com/base-android/generator-api/internal/config"
	"github.com/base-android/generator-api/internal/generate"
)

// selfTest proves an image can do its job: it loads the catalogue and generates one small project
// per enabled target through the same checks and generator as a request, then checks each zip
// opens. Only `server -selftest` runs it; neither the image's command nor CI does.
func selfTest(log *slog.Logger) error {
	cfg, err := config.Load()
	if err != nil {
		return err
	}
	workRoot, err := os.MkdirTemp("", "generator-selftest-")
	if err != nil {
		return err
	}
	defer func() { _ = os.RemoveAll(workRoot) }()

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Minute)
	defer cancel()
	catalogue, enabled, err := generators(ctx, ctx, cfg, options(cfg, workRoot), log)
	if err != nil {
		return err
	}

	var failed []string
	for _, target := range catalogue.Targets {
		g := enabled[target.Key]
		if g == nil {
			continue
		}
		entries, err := generateOne(ctx, g)
		if err != nil {
			log.Error("selftest", "target", target.Key, "error", err)
			failed = append(failed, target.Key)
			continue
		}
		log.Info("selftest", "target", target.Key, "zip_entries", entries)
	}
	if failed != nil {
		return fmt.Errorf("selftest failed for %v", failed)
	}
	log.Info("selftest passed", "targets", len(enabled), "generator_commit", catalogue.GeneratorCommit)
	return nil
}

// selftestSpec is the smallest project the target makes, with each locked option at its locked
// value, so a locked deployment proves it can generate what it serves.
func selftestSpec(target generate.TargetEntry) ([]byte, error) {
	spec := map[string]any{
		"schema":       2,
		"platform":     target.Key,
		"app_name":     "Selftest",
		"package_name": "com.example.selftest",
		"features":     target.SmallestPresetFeatures(),
	}
	for key, value := range target.LockedValues() {
		spec[key] = value
	}
	return json.Marshal(spec)
}

func generateOne(ctx context.Context, g *generate.Generator) (int, error) {
	spec, err := selftestSpec(g.Target)
	if err != nil {
		return 0, err
	}
	prechecked, err := generate.Precheck(spec, g.Target.Key, g.Rules)
	if err == nil {
		err = g.Target.CheckOptions(prechecked)
	}
	if err != nil {
		return 0, fmt.Errorf("the generate route would refuse the selftest's spec: %w", err)
	}
	out, err := g.Run(ctx, spec)
	var failure *generate.Failure
	if errors.As(err, &failure) {
		return 0, fmt.Errorf("%w\n%s", err, failure.Detail)
	}
	if err != nil {
		return 0, err
	}
	defer func() { _ = out.Close() }()

	archive, err := zip.OpenReader(out.ZipPath)
	if err != nil {
		return 0, fmt.Errorf("the zip does not open: %w", err)
	}
	defer func() { _ = archive.Close() }()
	if len(archive.File) == 0 {
		return 0, fmt.Errorf("the zip is empty")
	}
	return len(archive.File), nil
}
