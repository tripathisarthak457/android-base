package telemetry

import (
	"encoding/json"
	"net/http"
	"os"
	"testing"
)

type headerCase struct {
	Name     string            `json:"name"`
	Headers  map[string]string `json:"headers"`
	Expected struct {
		SessionID *string `json:"session_id"`
		ConfigMs  *int    `json:"config_ms"`
		Client    string  `json:"client"`
	} `json:"expected"`
}

// The website's request.ts tests read the same file, so the two sides of the session headers are
// checked against one list of cases.
func TestSessionHeaderContract(t *testing.T) {
	raw, err := os.ReadFile("../../testdata/contract/generate_headers.json")
	if err != nil {
		t.Fatal(err)
	}
	var fixture struct {
		Cases []headerCase `json:"cases"`
	}
	if err := json.Unmarshal(raw, &fixture); err != nil {
		t.Fatal(err)
	}
	if len(fixture.Cases) < 10 {
		t.Fatalf("only %d cases in the fixture", len(fixture.Cases))
	}

	for _, c := range fixture.Cases {
		t.Run(c.Name, func(t *testing.T) {
			h := http.Header{}
			for name, value := range c.Headers {
				h.Set(name, value)
			}
			got := ReadSession(h)

			wantID := ""
			if c.Expected.SessionID != nil {
				wantID = *c.Expected.SessionID
			}
			if got.ID != wantID {
				t.Errorf("session id = %q, want %q", got.ID, wantID)
			}
			switch {
			case c.Expected.ConfigMs == nil && got.ConfigMs != nil:
				t.Errorf("config ms = %d, want none", *got.ConfigMs)
			case c.Expected.ConfigMs != nil && (got.ConfigMs == nil || *got.ConfigMs != *c.Expected.ConfigMs):
				t.Errorf("config ms = %v, want %d", got.ConfigMs, *c.Expected.ConfigMs)
			}
			if string(got.Client) != c.Expected.Client {
				t.Errorf("client = %q, want %q", got.Client, c.Expected.Client)
			}
		})
	}
}
