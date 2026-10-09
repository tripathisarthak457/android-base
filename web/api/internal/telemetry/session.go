// Package telemetry turns request headers and client events into the closed, anonymous values the
// store records. Nothing here keeps an address or a raw user agent.
package telemetry

import (
	"net/http"
	"regexp"
	"strconv"
	"strings"
)

// Outcome is how one generate request ended. A closed set, because the admin portal groups by it.
type Outcome string

const (
	OK                  Outcome = "ok"
	Invalid             Outcome = "invalid"
	InternalError       Outcome = "internal_error"
	Timeout             Outcome = "timeout"
	AbandonedQueue      Outcome = "abandoned_queue"
	AbandonedRunning    Outcome = "abandoned_running"
	DownloadInterrupted Outcome = "download_interrupted"
	RateLimited         Outcome = "rate_limited"
)

// Outcomes lists every outcome in the order the admin portal shows them.
var Outcomes = []Outcome{
	OK, DownloadInterrupted, Invalid, InternalError, Timeout, AbandonedQueue, AbandonedRunning, RateLimited,
}

// Client says who made a request: the website, a script or terminal, or a crawler.
type Client string

const (
	ClientWeb Client = "web"
	ClientCLI Client = "cli"
	ClientBot Client = "bot"
)

// MaxConfigMs caps the time a visitor can have spent in the configurator. Two hours is already a
// tab left open over lunch; a larger value says nothing about how long configuring took.
const MaxConfigMs = 2 * 60 * 60 * 1000

// siteClientPrefix is how the website names itself in X-Client: `android-base-web/<build>`.
const siteClientPrefix = "android-base-web/"

var uuidV4 = regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)
var digits = regexp.MustCompile(`^[0-9]{1,7}$`)

// Session is what the generate route keeps from the session headers. A header that fails its check
// is ignored rather than rejected: the spec is what matters, and a script that sends a malformed
// session id should still get its project.
type Session struct {
	// ID is a lowercase UUID v4, or empty.
	ID string
	// ConfigMs is nil when the header was absent or out of range.
	ConfigMs *int
	Client   Client
}

// ReadSession reads the session headers and classifies the client.
func ReadSession(h http.Header) Session {
	return Session{
		ID:       SessionID(h.Get("X-Session-Id")),
		ConfigMs: configMs(h.Get("X-Config-Ms")),
		Client:   ClassifyClient(h.Get("X-Client"), h.Get("User-Agent")),
	}
}

// SessionID returns the id in its stored form, or "" when raw is not a UUID v4.
func SessionID(raw string) string {
	id := strings.ToLower(raw)
	if !uuidV4.MatchString(id) {
		return ""
	}
	return id
}

func configMs(raw string) *int {
	if !digits.MatchString(raw) {
		return nil
	}
	ms, err := strconv.Atoi(raw)
	if err != nil || ms > MaxConfigMs {
		return nil
	}
	return &ms
}

// ClassifyClient decides web, bot or cli. The site names itself; anything else is a crawler when
// its user agent says so, and a script otherwise.
func ClassifyClient(xClient, userAgent string) Client {
	if validClientHeader(xClient) && strings.HasPrefix(xClient, siteClientPrefix) && len(xClient) > len(siteClientPrefix) {
		return ClientWeb
	}
	if IsBot(userAgent) {
		return ClientBot
	}
	return ClientCLI
}

func validClientHeader(value string) bool {
	if value == "" || len(value) > 64 {
		return false
	}
	for i := 0; i < len(value); i++ {
		if value[i] < 0x20 || value[i] > 0x7e {
			return false
		}
	}
	return true
}
