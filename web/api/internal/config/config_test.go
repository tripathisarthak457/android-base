package config

import (
	"slices"
	"testing"
)

func TestParseProxyPolicy(t *testing.T) {
	cases := []struct {
		raw  string
		want ProxyPolicy
		ok   bool
	}{
		{"", ProxyPolicy{}, true},
		{"none", ProxyPolicy{}, true},
		{"vercel", ProxyPolicy{Hops: 1, Vercel: true}, true},
		{"hops=2", ProxyPolicy{Hops: 2}, true},
		{" hops=1 ", ProxyPolicy{Hops: 1}, true},
		{"hops=0", ProxyPolicy{}, false},
		{"hops=x", ProxyPolicy{}, false},
		{"caddy", ProxyPolicy{}, false},
	}
	for _, c := range cases {
		got, err := ParseProxyPolicy(c.raw)
		if (err == nil) != c.ok || got != c.want {
			t.Errorf("ParseProxyPolicy(%q) = %+v, %v", c.raw, got, err)
		}
	}
}

func TestLoadRefusesContradictions(t *testing.T) {
	t.Setenv("DATABASE_URL", "postgres://localhost/x")
	t.Setenv("ADMIN_TOKEN", "")
	t.Setenv("IP_SALT", "")
	t.Setenv("TRUSTED_PROXY", "everyone")
	if _, err := Load(); err == nil {
		t.Fatal("a database without a token, a salt or a valid proxy policy was accepted")
	}
}

func TestLoadDefaults(t *testing.T) {
	for _, key := range []string{"DATABASE_URL", "ADMIN_TOKEN", "IP_SALT", "TRUSTED_PROXY", "ENABLE_TARGETS", "PORT", "ADDR"} {
		t.Setenv(key, "")
	}
	cfg, err := Load()
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Addr != "127.0.0.1:8080" || cfg.TrustedProxy != (ProxyPolicy{}) ||
		!slices.Equal(cfg.EnableTargets, []string{"android"}) || cfg.RetainNames.Hours() != 90*24 {
		t.Fatalf("defaults: %+v", cfg)
	}
}
