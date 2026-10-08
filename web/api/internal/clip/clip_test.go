package clip

import (
	"strings"
	"testing"
	"unicode/utf8"
)

func TestStringNeverSplitsARune(t *testing.T) {
	// "नमस्ते" is six runes of three bytes each; every limit that lands inside one must back off.
	hindi := strings.Repeat("नमस्ते", 50)
	for limit := 0; limit < 40; limit++ {
		got := String(hindi, limit)
		if !utf8.ValidString(got) {
			t.Fatalf("limit %d produced invalid UTF-8: %q", limit, got)
		}
		if len(got) > limit {
			t.Fatalf("limit %d produced %d bytes", limit, len(got))
		}
		if limit-len(got) >= 3 {
			t.Fatalf("limit %d dropped a whole rune it had room for (%d bytes kept)", limit, len(got))
		}
	}
}

func TestStringDropsInvalidBytes(t *testing.T) {
	if got := String("ok\xff\xfe", 100); got != "ok" {
		t.Fatalf("got %q", got)
	}
}

func TestStringLeavesShortTextAlone(t *testing.T) {
	if got := String("Café", 5); got != "Café" {
		t.Fatalf("got %q", got)
	}
}
