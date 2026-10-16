package main

import (
	"fmt"
	"strings"
	"unicode/utf8"
)

func truncate(s string, limit int) string {
	if len(s) <= limit {
		return s
	}
	return s[:limit] + "…"
}

func main() {
	title := strings.Repeat("क", 67) // 67 chars, 201 bytes, under maxLength=200 UTF-16 units
	fmt.Println(len(title), utf8.ValidString(truncate(title, 200)))
	body := strings.Repeat("क", 2700)
	fmt.Println(len(body), utf8.ValidString(truncate(body, 8000)))
	ip := func(h string) string {
		if i := strings.LastIndex(h, ":"); i > 0 && !strings.Contains(h, "]") {
			return h[:i]
		}
		return h
	}
	fmt.Println(ip("2001:db8::1"), ip("2001:db8::2"))
}
