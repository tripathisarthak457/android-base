package httpapi

import (
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
)

func optionsServer() *Server {
	catalogue := json.RawMessage(`{"features":[]}`)
	return &Server{catalogue: catalogue, catalogueTag: fmt.Sprintf(`"%x"`, sha256.Sum256(catalogue))}
}

func TestOptionsAreRevalidatedAndCarryAnETag(t *testing.T) {
	s := optionsServer()
	recorder := httptest.NewRecorder()

	s.handleOptions(recorder, httptest.NewRequest(http.MethodGet, "/api/options", nil))

	if recorder.Code != http.StatusOK || recorder.Body.String() != `{"features":[]}` {
		t.Fatalf("got %d %q", recorder.Code, recorder.Body.String())
	}
	if got := recorder.Header().Get("Cache-Control"); got != "no-cache" {
		t.Fatalf("Cache-Control = %q, want no-cache", got)
	}
	if recorder.Header().Get("ETag") != s.catalogueTag {
		t.Fatalf("ETag = %q, want %q", recorder.Header().Get("ETag"), s.catalogueTag)
	}
}

func TestAnUnchangedCatalogueIsA304WithNoBody(t *testing.T) {
	s := optionsServer()
	request := httptest.NewRequest(http.MethodGet, "/api/options", nil)
	request.Header.Set("If-None-Match", s.catalogueTag)
	recorder := httptest.NewRecorder()

	s.handleOptions(recorder, request)

	if recorder.Code != http.StatusNotModified || recorder.Body.Len() != 0 {
		t.Fatalf("got %d with %d bytes", recorder.Code, recorder.Body.Len())
	}
}
