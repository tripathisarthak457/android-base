package store

import (
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"

	"github.com/base-android/generator-api/internal/telemetry"
)

func TestABatchIsOneStatementPerRowPlusTheRollup(t *testing.T) {
	b := &Batch{
		Generations: []Generation{{Target: "android", Outcome: telemetry.OK}},
		Events:      []Event{{Name: "generated"}, {Name: "downloaded"}},
		Visits:      []Visit{{SessionID: "x"}},
		Errors:      []ErrorRecord{{Kind: "generation", Message: "boom"}},
		Requests:    []Request{{Route: "generate"}},
	}
	var batch pgx.Batch
	b.queue(&batch)
	if batch.Len() != 7 {
		t.Fatalf("%d statements, want 7", batch.Len())
	}
	if !strings.Contains(batch.QueuedQueries[1].SQL, "daily_rollup") {
		t.Fatal("the rollup does not follow its generation")
	}
}

func TestQueueClipsFreeTextToValidUTF8(t *testing.T) {
	b := &Batch{Errors: []ErrorRecord{{Kind: "api", Message: strings.Repeat("ह", 600)}}}
	var batch pgx.Batch
	b.queue(&batch)
	message := batch.QueuedQueries[0].Arguments[2].(string)
	if len(message) > 1000 || !strings.HasPrefix(message, "ह") {
		t.Fatalf("message is %d bytes", len(message))
	}
}

func TestFingerprintIgnoresWhatVariesBetweenRuns(t *testing.T) {
	a := Fingerprint("generation", "android: KeyError at /tmp/generator-api-123/android-9/x.py line 41")
	b := Fingerprint("generation", "android: KeyError at /tmp/generator-api-777/android-2/x.py line 42")
	c := Fingerprint("generation", "android: ValueError at /tmp/generator-api-777/android-2/x.py line 42")
	if a != b {
		t.Fatal("the same bug in two runs got two fingerprints")
	}
	if a == c {
		t.Fatal("two different bugs share a fingerprint")
	}
}

func TestVisitorHashChangesEveryDay(t *testing.T) {
	s := &Store{salt: "salt"}
	day := time.Date(2026, 10, 9, 8, 0, 0, 0, time.UTC)
	morning := s.VisitorHash("203.0.113.7", day)
	evening := s.VisitorHash("203.0.113.7", day.Add(12*time.Hour))
	tomorrow := s.VisitorHash("203.0.113.7", day.Add(24*time.Hour))
	if morning != evening {
		t.Fatal("one visitor hashed differently within a day")
	}
	if morning == tomorrow {
		t.Fatal("a visitor hashed the same on two days, which links their visits")
	}
	if len(morning) != 32 || strings.Contains(morning, "203") {
		t.Fatalf("hash %q", morning)
	}
}
