// Package clip shortens text to a byte budget without producing text Postgres refuses.
package clip

import (
	"strings"
	"unicode/utf8"
)

// String returns s cut to at most limit bytes, on a rune boundary, with any invalid UTF-8 and any
// NUL removed.
//
// Postgres rejects a text value that is not valid UTF-8 or that contains NUL, and one refused value
// fails the whole batch: a byte slice through the middle of a Hindi bug report, or a crashed child
// that wrote a NUL to stderr, would lose the generation row along with its error-list entry.
func String(s string, limit int) string {
	s = strings.ReplaceAll(strings.ToValidUTF8(s, ""), "\x00", "")
	if len(s) <= limit {
		return s
	}
	for limit > 0 && !utf8.RuneStart(s[limit]) {
		limit--
	}
	return s[:limit]
}
