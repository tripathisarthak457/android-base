package telemetry

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"net/url"
	"slices"
	"strings"
	"unicode"

	"github.com/base-android/generator-api/internal/clip"
)

// Client events: the funnel steps the website reports. A closed set, so a public write path cannot
// be filled with whatever names a script invents.
const (
	EventLanded           = "landed"
	EventPlatformSelected = "platform_selected"
	EventStepViewed       = "step_viewed"
	EventPresetChosen     = "preset_chosen"
	EventGenerateClicked  = "generate_clicked"
	EventDownloadStarted  = "download_started"
)

// Server events, recorded by the generate route from what actually happened. The client cannot
// send them: a funnel whose last step the browser can claim is a funnel that always converts.
const (
	EventGenerated           = "generated"
	EventDownloaded          = "downloaded"
	EventDownloadInterrupted = "download_interrupted"
)

// ClientEvents and Steps are the accepted values, in funnel order.
var (
	ClientEvents = []string{
		EventLanded, EventPlatformSelected, EventStepViewed, EventPresetChosen, EventGenerateClicked, EventDownloadStarted,
	}
	Steps     = []string{"platform", "identity", "features", "look", "motion", "build", "review"}
	Viewports = []string{"xs", "sm", "md", "lg", "xl"}
)

// MaxEventsPerBatch bounds one POST. The site sends a handful at a time; more is a script.
const MaxEventsPerBatch = 20

// EventBatch is the body of POST /api/v1/events.
type EventBatch struct {
	SessionID string    `json:"session_id"`
	Events    []EventIn `json:"events"`
}

// EventIn is one event as the browser sends it. Only `landed` carries the acquisition fields.
type EventIn struct {
	Name   string `json:"name"`
	Target string `json:"target,omitempty"`
	Step   string `json:"step,omitempty"`
	Preset string `json:"preset,omitempty"`

	Referrer    string `json:"referrer,omitempty"`
	UTMSource   string `json:"utm_source,omitempty"`
	UTMMedium   string `json:"utm_medium,omitempty"`
	UTMCampaign string `json:"utm_campaign,omitempty"`
	Returning   bool   `json:"returning,omitempty"`
	Viewport    string `json:"viewport,omitempty"`
}

// Event is a validated funnel event ready to store. Value holds the preset of `preset_chosen`.
type Event struct {
	Name   string
	Target string
	Step   string
	Value  string
}

// Landing is what a `landed` event says about how the visitor arrived.
type Landing struct {
	ReferrerOrigin string
	UTMSource      string
	UTMMedium      string
	UTMCampaign    string
	Returning      bool
	Viewport       string
}

// Known is the catalogue as event validation needs it: each target's preset keys.
type Known map[string][]string

// Validated is a batch that passed every check.
type Validated struct {
	SessionID string
	Events    []Event
	// Landing is nil unless the batch held a `landed` event.
	Landing *Landing
}

// ErrInvalidEvents wraps every reason a batch is refused.
var ErrInvalidEvents = errors.New("invalid events")

func invalid(format string, args ...any) error {
	return fmt.Errorf("%w: %s", ErrInvalidEvents, fmt.Sprintf(format, args...))
}

// DecodeEvents reads a batch strictly: a field this API does not know is refused, so the site and
// the API cannot drift apart without a test noticing.
func DecodeEvents(body []byte) (EventBatch, error) {
	var batch EventBatch
	decoder := json.NewDecoder(bytes.NewReader(body))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&batch); err != nil {
		return batch, invalid("%v", err)
	}
	return batch, nil
}

// ValidateEvents checks a batch against the closed enums and the catalogue. selfHost is the host the
// site is served from, so moving between its own pages is not counted as a referral.
func ValidateEvents(batch EventBatch, known Known, selfHost string) (Validated, error) {
	out := Validated{SessionID: SessionID(batch.SessionID)}
	if out.SessionID == "" {
		return out, invalid("session_id must be a UUID v4")
	}
	if len(batch.Events) == 0 || len(batch.Events) > MaxEventsPerBatch {
		return out, invalid("send between 1 and %d events", MaxEventsPerBatch)
	}

	for i, in := range batch.Events {
		if !slices.Contains(ClientEvents, in.Name) {
			return out, invalid("event %d: unknown name %q", i, in.Name)
		}
		event := Event{Name: in.Name}

		if in.Name == EventLanded {
			if in.Target != "" || in.Step != "" || in.Preset != "" {
				return out, invalid("event %d: landed comes before a platform is chosen", i)
			}
			if out.Landing != nil {
				return out, invalid("event %d: one landed per batch", i)
			}
			landing, err := landingFrom(in, selfHost)
			if err != nil {
				return out, invalid("event %d: %v", i, err)
			}
			out.Landing = &landing
			out.Events = append(out.Events, event)
			continue
		}

		if in.Referrer != "" || in.UTMSource != "" || in.UTMMedium != "" || in.UTMCampaign != "" ||
			in.Returning || in.Viewport != "" {
			return out, invalid("event %d: only landed carries acquisition fields", i)
		}
		presets, ok := known[in.Target]
		if !ok {
			return out, invalid("event %d: unknown target %q", i, in.Target)
		}
		event.Target = in.Target

		switch in.Name {
		case EventStepViewed:
			if !slices.Contains(Steps, in.Step) {
				return out, invalid("event %d: unknown step %q", i, in.Step)
			}
			event.Step = in.Step
		case EventPresetChosen:
			if !slices.Contains(presets, in.Preset) {
				return out, invalid("event %d: %q is not a %s preset", i, in.Preset, in.Target)
			}
			event.Value = in.Preset
		}
		if in.Step != "" && in.Name != EventStepViewed || in.Preset != "" && in.Name != EventPresetChosen {
			return out, invalid("event %d: %s takes no step or preset", i, in.Name)
		}
		out.Events = append(out.Events, event)
	}
	return out, nil
}

func landingFrom(in EventIn, selfHost string) (Landing, error) {
	landing := Landing{
		ReferrerOrigin: ReferrerOrigin(in.Referrer, selfHost),
		Returning:      in.Returning,
	}
	if in.Viewport != "" {
		if !slices.Contains(Viewports, in.Viewport) {
			return landing, fmt.Errorf("unknown viewport %q", in.Viewport)
		}
		landing.Viewport = in.Viewport
	}
	for _, field := range []struct {
		value string
		into  *string
	}{
		{in.UTMSource, &landing.UTMSource},
		{in.UTMMedium, &landing.UTMMedium},
		{in.UTMCampaign, &landing.UTMCampaign},
	} {
		value := strings.TrimSpace(field.value)
		if strings.IndexFunc(value, unicode.IsControl) >= 0 {
			return landing, errors.New("UTM values cannot contain control characters")
		}
		*field.into = clip.String(value, 64)
	}
	return landing, nil
}

// ReferrerOrigin keeps the scheme and host of a referrer and drops the path: knowing that traffic
// came from a forum is useful, recording which thread is more than this needs to know. It returns ""
// for anything that is not an http(s) URL and for the site's own pages.
func ReferrerOrigin(raw, selfHost string) string {
	if raw == "" || len(raw) > 2048 {
		return ""
	}
	parsed, err := url.Parse(raw)
	if err != nil || parsed.Host == "" || (parsed.Scheme != "http" && parsed.Scheme != "https") {
		return ""
	}
	host := strings.ToLower(parsed.Host)
	if selfHost != "" && host == strings.ToLower(selfHost) {
		return ""
	}
	return clip.String(parsed.Scheme+"://"+host, 100)
}
