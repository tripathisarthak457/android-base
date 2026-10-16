package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"time"
)

func main() {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		_ = json.NewEncoder(w).Encode(map[string]bool{"ok": true})
		time.Sleep(400 * time.Millisecond) // stands in for RecordRequest after next()
	}))
	defer srv.Close()
	start := time.Now()
	resp, err := http.Get(srv.URL)
	if err != nil {
		panic(err)
	}
	fmt.Println("headers after", time.Since(start).Round(time.Millisecond))
	resp.Body.Close()
}
