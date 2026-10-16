package main

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"time"
)

// Copied from web/api/internal/store/store.go VisitorHash (salt fixed).
func visitorHash(remoteAddr string) string {
	host := remoteAddr
	if index := strings.LastIndex(host, ":"); index > 0 && !strings.Contains(host, "]") {
		host = host[:index]
	}
	sum := sha256.Sum256([]byte("salt|" + host))
	return host + " -> " + hex.EncodeToString(sum[:4])
}

func main() {
	for _, ip := range []string{"203.0.113.7", "2001:db8:1:2:3:4:5:1", "2001:db8:1:2:3:4:5:2", "2001:db8::1"} {
		fmt.Println("hash input", ip, "=>", visitorHash(ip))
	}

	// observe-style: handler writes small JSON, then a 400ms "DB write" before returning.
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		_ = json.NewEncoder(w).Encode(map[string]any{"ok": true})
		time.Sleep(400 * time.Millisecond)
	}))
	defer srv.Close()
	start := time.Now()
	resp, err := http.Get(srv.URL)
	if err != nil {
		panic(err)
	}
	headers := time.Since(start)
	_, _ = io.ReadAll(resp.Body)
	fmt.Printf("small JSON: headers after %v, body complete after %v\n", headers.Round(time.Millisecond), time.Since(start).Round(time.Millisecond))
}
