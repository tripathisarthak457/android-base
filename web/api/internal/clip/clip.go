// Package clip shortens text to a byte budget without producing invalid UTF-8.
package clip

import (
	"strings"
	"unicode/utf8"
)

// String returns s cut to at most limit bytes, on a rune boundary, with any invalid UTF-8 removed.
//
// Postgres rejects a text value that is not valid UTF-8, so a byte slice through the middle of a
// Hindi bug report or a Python traceback with a non-ASCII path fails the whole insert.
func String(s string, limit int) string {
	s = strings.ToValidUTF8(s, "")
	if len(s) <= limit {
		return s
	}
	for limit > 0 && !utf8.RuneStart(s[limit]) {
		limit--
	}
	return s[:limit]
}
