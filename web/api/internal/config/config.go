// Package config reads the service's settings from the environment. Everything has a default that
// works on a laptop, so `go run ./cmd/server` starts without a single variable set.
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
	// Addr is the listen address. Locally this stays on loopback; in a container it has to be
	// reachable from outside the container, which is what defaultAddr works out.
	Addr string

	// DatabaseURL is a libpq connection string. Empty disables persistence entirely: the API
	// still generates projects, it just records nothing. That is the right behaviour for a
	// laptop and the wrong one for production, so it is logged loudly at boot.
	DatabaseURL string

	// GeneratorDir holds generate_headless.py and the genkit package.
	GeneratorDir string

	// CatalogueFile is the catalogue precomputed when the image was built. Empty runs the generator
	// once at boot to produce it.
	CatalogueFile string

	// EnableTargets are the targets this deployment generates. A target the catalogue lists but this
	// does not name is shown on the site and refused by the generate route, which is how `ios`
	// stays "coming soon" while its generator exists.
	EnableTargets []string

	// TrustedProxy says which X-Forwarded-For entry is the client. See ProxyPolicy.
	TrustedProxy ProxyPolicy

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

	// MaxConcurrentGenerations caps how many Python processes run at once, per target. Each is
	// short but CPU- and IO-heavy, and an unbounded number on a small instance is how it dies rather
	// than slows down.
	MaxConcurrentGenerations int

	// RateLimitPerHour is per client IP, counted in one process. See the note on `limiter`:
	// across several instances the real ceiling is this times the instance count.
	RateLimitPerHour int

	// IPSalt is mixed, with the date, into the hash used for unique-visitor counts, so the same
	// visitor hashes differently every day and the database never holds a stable pseudonym.
	IPSalt string

	// Retention for requests, events and visits, and for app and package names on generations.
	RetainRequests time.Duration
	RetainEvents   time.Duration
	RetainNames    time.Duration
}

// ProxyPolicy decides which address is the client's. Every proxy that appends to X-Forwarded-For
// adds the address it received the request from, so with N such proxies in front of
// the server the client is the Nth entry from the right; anything further left was written by the
// client and proves nothing.
type ProxyPolicy struct {
	// Hops is the number of trusted proxies. Zero ignores X-Forwarded-For and uses the connection.
	Hops int
	// Vercel's edge replaces X-Forwarded-For with the client address and adds the geo headers, which
	// are trusted only under this policy.
	Vercel bool
}

func (p ProxyPolicy) String() string {
	switch {
	case p.Vercel:
		return "vercel"
	case p.Hops == 0:
		return "none"
	default:
		return fmt.Sprintf("hops=%d", p.Hops)
	}
}

// ParseProxyPolicy reads TRUSTED_PROXY: `none`, `vercel`, or `hops=N`.
func ParseProxyPolicy(raw string) (ProxyPolicy, error) {
	switch raw = strings.TrimSpace(raw); {
	case raw == "" || raw == "none":
		return ProxyPolicy{}, nil
	case raw == "vercel":
		return ProxyPolicy{Hops: 1, Vercel: true}, nil
	case strings.HasPrefix(raw, "hops="):
		hops, err := strconv.Atoi(strings.TrimPrefix(raw, "hops="))
		if err != nil || hops < 1 || hops > 10 {
			return ProxyPolicy{}, fmt.Errorf("TRUSTED_PROXY %q: hops must be between 1 and 10", raw)
		}
		return ProxyPolicy{Hops: hops}, nil
	default:
		return ProxyPolicy{}, fmt.Errorf("TRUSTED_PROXY %q: use none, vercel or hops=N", raw)
	}
}

func Load() (Config, error) {
	cfg := Config{
		Addr:                     env("ADDR", defaultAddr()),
		DatabaseURL:              os.Getenv("DATABASE_URL"),
		GeneratorDir:             env("GENERATOR_DIR", "../../generator"),
		CatalogueFile:            os.Getenv("CATALOGUE_FILE"),
		EnableTargets:            splitAndTrim(env("ENABLE_TARGETS", "android,multiplatform")),
		PythonBin:                env("PYTHON_BIN", defaultPython()),
		AdminToken:               os.Getenv("ADMIN_TOKEN"),
		AllowedOrigins:           splitAndTrim(env("ALLOWED_ORIGINS", "http://localhost:3000")),
		GenerateTimeout:          duration("GENERATE_TIMEOUT", 90*time.Second),
		MaxConcurrentGenerations: integer("MAX_CONCURRENT_GENERATIONS", 4),
		RateLimitPerHour:         integer("RATE_LIMIT_PER_HOUR", 30),
		IPSalt:                   env("IP_SALT", ""),
		RetainRequests:           days("RETENTION_REQUESTS_DAYS", 30),
		RetainEvents:             days("RETENTION_EVENTS_DAYS", 400),
		RetainNames:              days("RETENTION_NAMES_DAYS", 90),
	}

	var problems []string
	proxy, err := ParseProxyPolicy(os.Getenv("TRUSTED_PROXY"))
	if err != nil {
		problems = append(problems, err.Error())
	}
	cfg.TrustedProxy = proxy
	if len(cfg.EnableTargets) == 0 {
		problems = append(problems, "ENABLE_TARGETS must name at least one target")
	}
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

// defaultAddr picks a listen address from the environment the process finds itself in. A container
// platform hands the port over in PORT and expects the server on every interface — loopback there
// means the health check never connects and the deployment is rolled back with no useful error.
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

func days(key string, fallback int) time.Duration {
	return time.Duration(integer(key, fallback)) * 24 * time.Hour
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
		"addr=%s generator=%s python=%s persistent=%t origins=%v targets=%v proxy=%s concurrency=%d",
		c.Addr, c.GeneratorDir, c.PythonBin, c.Persistent(), c.AllowedOrigins, c.EnableTargets,
		c.TrustedProxy, c.MaxConcurrentGenerations,
	)
}
