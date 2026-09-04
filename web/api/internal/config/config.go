// Package config reads the service's settings from the environment.
//
// Everything has a default that works on a laptop, so `go run ./cmd/server` starts without a
// single variable set. The two that have no sensible default — the database URL and the admin
// token — fail loudly at boot rather than at the first request that needs them, because a service
// that starts and then 500s on its third endpoint is much harder to diagnose than one that
// refuses to start.
package config

import (
	"errors"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	// Addr is the listen address. Behind Caddy on a VPS this stays on loopback; in a container
	// it has to be reachable from outside the container, which is what defaultAddr works out.
	Addr string

	// DatabaseURL is a libpq connection string. Empty disables persistence entirely: the API
	// still generates projects, it just records nothing. That is the right behaviour for a
	// laptop and the wrong one for production, so it is logged loudly at boot.
	DatabaseURL string

	// GeneratorDir holds generate_headless.py and the genkit package.
	GeneratorDir string

	// PythonBin is the interpreter. `python3` everywhere except Windows.
	PythonBin string

	// AdminToken guards /admin/*. Required whenever DatabaseURL is set, because an admin portal
	// with no auth in front of a database is worse than no admin portal.
	AdminToken string

	// AllowedOrigins is the CORS allow-list for the browser app. Exact matches only; there is no
	// wildcard, because the download endpoint is a POST that returns a file and a permissive
	// origin policy on it is an invitation.
	AllowedOrigins []string

	// GenerateTimeout bounds one generation. Measured at ~3s for a full-feature project on a
	// modest box; anything past this is a hang, not a slow run.
	GenerateTimeout time.Duration

	// MaxConcurrentGenerations caps how many Python processes run at once. Each is short but
	// CPU- and IO-heavy, and an unbounded queue on a 1GB VPS is how the box dies rather than
	// slows down.
	MaxConcurrentGenerations int

	// RateLimitPerHour is per client IP, counted in one process. See the note on `limiter`:
	// across several instances the real ceiling is this times the instance count.
	RateLimitPerHour int

	// IPSalt is mixed into the hash used for unique-visitor counts. Rotating it forgets who
	// visited yesterday, which is the point: the counts survive, the identities do not.
	IPSalt string
}

func Load() (Config, error) {
	cfg := Config{
		Addr:                     env("ADDR", defaultAddr()),
		DatabaseURL:              os.Getenv("DATABASE_URL"),
		GeneratorDir:             env("GENERATOR_DIR", "../../generator"),
		PythonBin:                env("PYTHON_BIN", defaultPython()),
		AdminToken:               os.Getenv("ADMIN_TOKEN"),
		AllowedOrigins:           splitAndTrim(env("ALLOWED_ORIGINS", "http://localhost:3000")),
		GenerateTimeout:          duration("GENERATE_TIMEOUT", 90*time.Second),
		MaxConcurrentGenerations: integer("MAX_CONCURRENT_GENERATIONS", 4),
		RateLimitPerHour:         integer("RATE_LIMIT_PER_HOUR", 30),
		IPSalt:                   env("IP_SALT", ""),
	}

	var problems []string
	if cfg.DatabaseURL != "" && cfg.AdminToken == "" {
		problems = append(problems, "ADMIN_TOKEN must be set when DATABASE_URL is")
	}
	if cfg.AdminToken != "" && len(cfg.AdminToken) < 24 {
		problems = append(problems, "ADMIN_TOKEN must be at least 24 characters")
	}
	if cfg.DatabaseURL != "" && cfg.IPSalt == "" {
		problems = append(problems, "IP_SALT must be set when DATABASE_URL is")
	}
	if len(cfg.AllowedOrigins) == 0 {
		problems = append(problems, "ALLOWED_ORIGINS must name at least one origin")
	}
	if problems != nil {
		return cfg, errors.New(strings.Join(problems, "; "))
	}
	return cfg, nil
}

// Persistent reports whether anything is being recorded. The admin endpoints are not registered
// at all when it is false, rather than registered and returning empty results.
func (c Config) Persistent() bool { return c.DatabaseURL != "" }

// defaultAddr picks a listen address from the environment the process finds itself in.
//
// A container platform hands the port over in PORT and expects the server on every interface —
// loopback there means the health check never connects and the deployment is rolled back with no
// useful error. Everywhere else loopback is the safe default: on the VPS, Caddy is the only thing
// that should be able to reach this port. ADDR still overrides both.
func defaultAddr() string {
	if port := strings.TrimSpace(os.Getenv("PORT")); port != "" {
		return "0.0.0.0:" + port
	}
	return "127.0.0.1:8080"
}

func env(key, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(key)); value != "" {
		return value
	}
	return fallback
}

func integer(key string, fallback int) int {
	value, err := strconv.Atoi(os.Getenv(key))
	if err != nil || value <= 0 {
		return fallback
	}
	return value
}

func duration(key string, fallback time.Duration) time.Duration {
	value, err := time.ParseDuration(os.Getenv(key))
	if err != nil || value <= 0 {
		return fallback
	}
	return value
}

func splitAndTrim(raw string) []string {
	parts := strings.Split(raw, ",")
	out := make([]string, 0, len(parts))
	for _, part := range parts {
		if trimmed := strings.TrimSpace(part); trimmed != "" {
			out = append(out, trimmed)
		}
	}
	return out
}

func defaultPython() string {
	if os.PathSeparator == '\\' {
		return "py"
	}
	return "python3"
}

func (c Config) String() string {
	return fmt.Sprintf(
		"addr=%s generator=%s python=%s persistent=%t origins=%v concurrency=%d",
		c.Addr, c.GeneratorDir, c.PythonBin, c.Persistent(), c.AllowedOrigins,
		c.MaxConcurrentGenerations,
	)
}
