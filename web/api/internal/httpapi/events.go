package httpapi

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/base-android/generator-api/internal/store"
	"github.com/base-android/generator-api/internal/telemetry"
)

// handleEvents records a batch of the website's funnel events. A `landed` event also writes the
// session's visit: how it arrived, roughly from where, on what kind of device.
func (s *Server) handleEvents(w http.ResponseWriter, r *http.Request) {
	body, ok := readBody(w, r, 8<<10)
	if !ok {
		return
	}
	batch, err := telemetry.DecodeEvents(body)
	var validated telemetry.Validated
	if err == nil {
		validated, err = telemetry.ValidateEvents(batch, s.known, s.siteHost(r))
	}
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid_events", err.Error(), "")
		return
	}
	if s.sink == nil {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	client := telemetry.ClassifyClient(r.Header.Get("X-Client"), r.UserAgent())
	out := recordings(r)
	for _, e := range validated.Events {
		out.Events = append(out.Events, store.Event{
			SessionID: validated.SessionID, Target: e.Target, Name: e.Name, Step: e.Step, Value: e.Value, Client: client,
		})
	}
	if validated.Landing != nil {
		out.Visits = append(out.Visits, s.visit(r, validated.SessionID, *validated.Landing, client))
	}
	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) visit(r *http.Request, sessionID string, landing telemetry.Landing, client telemetry.Client) store.Visit {
	return store.Visit{
		SessionID:   sessionID,
		VisitorHash: s.store.VisitorHash(s.clientKey(r), time.Now()),
		Landing:     landing,
		Geo:         s.geo(r),
		Agent:       telemetry.ClassifyAgent(r.UserAgent(), r.Header.Get("Sec-CH-UA-Mobile")),
		Client:      client,
	}
}

// legacySteps maps the steps the previous site sent to /api/track onto the funnel's events.
var legacySteps = map[string]string{
	"landed":     telemetry.EventLanded,
	"configured": telemetry.EventGenerateClicked,
	"downloaded": telemetry.EventDownloadStarted,
}

// handleTrack is the pre-v1 funnel endpoint, kept for one release. It has no session, so its events
// count towards totals but never join a funnel.
func (s *Server) handleTrack(w http.ResponseWriter, r *http.Request) {
	body, ok := readBody(w, r, 1<<10)
	if !ok {
		return
	}
	var payload struct {
		Step string `json:"step"`
	}
	if err := json.Unmarshal(body, &payload); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_json", "That request was not valid JSON.", "")
		return
	}
	name, known := legacySteps[payload.Step]
	if !known {
		writeError(w, http.StatusBadRequest, "invalid_events", "Unknown step.", "step")
		return
	}
	if s.sink == nil {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	client := telemetry.ClassifyClient(r.Header.Get("X-Client"), r.UserAgent())
	out := recordings(r)
	target := "android"
	if name == telemetry.EventLanded {
		target = ""
		out.Visits = append(out.Visits, s.visit(r, "", telemetry.Landing{}, client))
	}
	out.Events = append(out.Events, store.Event{Target: target, Name: name, Client: client})
	w.WriteHeader(http.StatusNoContent)
}
