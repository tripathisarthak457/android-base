package httpapi

import (
	"cmp"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"slices"
	"strconv"
	"strings"
	"time"

	"github.com/base-android/generator-api/internal/generate"
	"github.com/base-android/generator-api/internal/store"
	"github.com/base-android/generator-api/internal/telemetry"
)

// glassFeature is the feature key behind the optional liquid glass chrome.
const glassFeature = "liquidglass"

// generate answers POST /api/v1/{target}/generate (and the legacy /api/generate as android): the spec
// is checked for size, shape and the target's locked options, passed to the generator unchanged, and
// the zip streamed back. A
// request for an enabled target is recorded with its outcome unless its body could not be read; a
// client over the rate limit is recorded once per window, not once per refusal.
func (s *Server) generate(w http.ResponseWriter, r *http.Request, target string) {
	started := time.Now()
	gen := s.generators[target]
	if gen == nil {
		s.refuseTarget(w, target)
		return
	}

	session := telemetry.ReadSession(r.Header)
	client := s.clientKey(r)
	agent := telemetry.ClassifyAgent(r.UserAgent(), r.Header.Get("Sec-CH-UA-Mobile"))
	batch := recordings(r)
	record := store.Generation{
		Target:      target,
		SessionID:   session.ID,
		Client:      session.Client,
		ConfigMs:    session.ConfigMs,
		Country:     s.geo(r).Country,
		DeviceClass: agent.DeviceClass,
	}
	if s.store != nil {
		record.VisitorHash = s.store.VisitorHash(client, started)
	}
	finish := func(outcome telemetry.Outcome) {
		record.Outcome = outcome
		if record.Duration == 0 {
			record.Duration = time.Since(started)
		}
		batch.Generations = append(batch.Generations, record)
	}
	serverEvent := func(name string) {
		// Server events exist for the funnel, which is counted in sessions.
		if session.ID != "" {
			batch.Events = append(batch.Events, store.Event{
				SessionID: session.ID, Target: target, Name: name, Client: session.Client, ConfigMs: session.ConfigMs,
			})
		}
	}

	if ok, first := s.generateLimit.allow(client); !ok {
		if first {
			finish(telemetry.RateLimited)
		}
		w.Header().Set("Retry-After", "3600")
		writeError(w, http.StatusTooManyRequests, "rate_limited",
			"That is a lot of projects. Try again in an hour, or run the generator locally: it is the "+
				"same code and there is no limit on it.", "")
		return
	}

	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, int64(gen.Rules.SpecBytesLimit())))
	if err != nil {
		var tooLarge *http.MaxBytesError
		if errors.As(err, &tooLarge) {
			record.FailureReason = "The spec is too large."
			finish(telemetry.Invalid)
			writeError(w, http.StatusRequestEntityTooLarge, "too_large", "That spec is too large.", "")
			return
		}
		writeError(w, http.StatusBadRequest, "unreadable", "The request could not be read.", "")
		return
	}

	spec, err := generate.Precheck(body, target, gen.Rules)
	if err == nil {
		err = gen.Target.CheckOptions(spec)
	}
	var refused *generate.PrecheckError
	var locked *generate.OptionLockedError
	switch {
	case errors.As(err, &refused):
		record.FailureReason, record.FailureField = refused.Message, refused.Field
		finish(telemetry.Invalid)
		writeError(w, http.StatusBadRequest, "invalid_spec", refused.Message, refused.Field)
		return
	case errors.As(err, &locked):
		// A well-formed spec this deployment does not generate: recorded with what it asked for, so
		// the admin sees what people wanted while the option is locked.
		describe(&record, spec, &gen.Target, s.catalogue.GeneratorCommit)
		record.FailureReason, record.FailureField = locked.Note, locked.Field
		finish(telemetry.Invalid)
		writeError(w, http.StatusForbidden, "option_locked", locked.Note, locked.Field)
		return
	case err != nil:
		s.generationFailed(w, r, &record, err)
		finish(record.Outcome)
		return
	}
	describe(&record, spec, &gen.Target, s.catalogue.GeneratorCommit)

	record.ColdStart = !s.warm.Swap(true)
	out, err := gen.Run(r.Context(), body)
	record.Duration = time.Since(started)
	if err != nil {
		s.generationFailed(w, r, &record, err)
		finish(record.Outcome)
		return
	}
	defer func() { _ = out.Close() }()

	setFeatures(&record, &gen.Target, out.ResolvedFeatures)
	record.GeneratorCommit = cmp.Or(out.GeneratorCommit, record.GeneratorCommit)
	record.Notes, record.IgnoredFields, record.Warnings = out.Notes, out.IgnoredFields, out.Warnings
	record.ZipBytes = out.ZipBytes
	record.QueueMs = store.Milliseconds(out.QueueTime)
	record.PythonMs = store.Milliseconds(out.RunTime)
	serverEvent(telemetry.EventGenerated)

	file, err := os.Open(out.ZipPath)
	if err != nil {
		record.FailureReason = "the generated zip could not be opened"
		s.log.Error("opening the generated zip", "error", err)
		batch.Errors = append(batch.Errors, store.ErrorRecord{Kind: "generation", Message: err.Error(), Path: r.URL.Path})
		finish(telemetry.InternalError)
		writeError(w, http.StatusInternalServerError, "generator_failed", "The project was generated but could not be read.", "")
		return
	}
	defer func() { _ = file.Close() }()

	elapsed := out.ElapsedMs
	if elapsed == 0 {
		elapsed = int(out.RunTime.Milliseconds())
	}
	name := generate.ProjectFileName(out.ProjectName)
	w.Header().Set("Content-Type", "application/zip")
	w.Header().Set("Content-Disposition", `attachment; filename="`+name+`"`)
	w.Header().Set("Content-Length", strconv.FormatInt(out.ZipBytes, 10))
	w.Header().Set("X-Project-Name", strings.TrimSuffix(name, ".zip"))
	w.Header().Set("X-Generation-Ms", strconv.Itoa(elapsed))
	// Keys asked for and not produced: the zip is complete, but a visitor who was told they were
	// getting keys has to hear that they did not. The generator does not list them in its result
	// yet (it only explains them in warnings), so until it does this header is never sent.
	if skipped := identifiers(out.KeystoresSkipped); len(skipped) > 0 {
		w.Header().Set("X-Keystores-Skipped", strings.Join(skipped, ","))
	}
	w.WriteHeader(http.StatusOK)

	streamed := time.Now()
	_, err = io.Copy(w, file)
	record.StreamMs = store.Milliseconds(time.Since(streamed))
	if err != nil {
		s.log.Warn("download interrupted", "error", err, "target", target)
		serverEvent(telemetry.EventDownloadInterrupted)
		finish(telemetry.DownloadInterrupted)
		return
	}
	serverEvent(telemetry.EventDownloaded)
	finish(telemetry.OK)
}

// generationFailed answers a generation that produced no zip and notes why on the record. Only a
// rejection of the input reaches the visitor in the generator's words; anything else is the
// generator's fault, goes to the error list with its traceback, and gets a generic message.
func (s *Server) generationFailed(w http.ResponseWriter, r *http.Request, record *store.Generation, err error) {
	var failure *generate.Failure
	if !errors.As(err, &failure) {
		failure = &generate.Failure{Outcome: telemetry.InternalError, Message: err.Error()}
	}
	record.Outcome = failure.Outcome
	record.QueueMs = store.Milliseconds(failure.QueueTime)
	if failure.Outcome != telemetry.AbandonedQueue {
		record.PythonMs = store.Milliseconds(failure.RunTime)
	}

	switch failure.Outcome {
	case telemetry.Invalid:
		record.FailureReason, record.FailureField = failure.Message, failure.Field
		writeError(w, http.StatusBadRequest, "invalid_spec", failure.Message, failure.Field)
	case telemetry.Timeout, telemetry.InternalError:
		record.FailureReason = failure.Message
		s.log.Error("generation failed", "target", record.Target, "outcome", failure.Outcome, "error", failure.Message)
		recordings(r).Errors = append(recordings(r).Errors, store.ErrorRecord{
			Kind:    "generation",
			Message: fmt.Sprintf("%s: %s", record.Target, failure.Message),
			Detail:  failure.Detail,
			Path:    r.URL.Path,
			// The generator words every internal failure the same way, so the message alone
			// would put every bug in one group.
			Fingerprint: store.Fingerprint("generation", record.Target, cmp.Or(failure.Signature, failure.Message)),
		})
		code := "generator_failed"
		if failure.Outcome == telemetry.Timeout {
			code = "timeout"
		}
		writeError(w, http.StatusInternalServerError, code,
			"The generator failed. This has been logged; the CLI in the repository does the same job.", "")
	default:
		record.FailureReason = failure.Message
		// Abandoned. Usually the visitor has gone and there is nobody to answer; one still connected
		// was cut off by a shutdown, and trying again reaches another instance.
		if r.Context().Err() == nil {
			writeError(w, http.StatusServiceUnavailable, "unavailable", "The server is restarting. Try again in a moment.", "")
		}
	}
}

// describe copies what the spec says onto the record, with the catalogue's defaults for the fields
// it left out, so "Utility" is counted whether it was picked or assumed. The features are resolved
// against the catalogue and the commit is the catalogue's: the admin compares failed runs with
// successful ones by feature, preset and commit, and a run that fails never reports its own.
func describe(record *store.Generation, spec generate.Spec, target *generate.TargetEntry, commit string) {
	record.AppName = spec.AppName
	record.PackageName = spec.PackageName
	setFeatures(record, target, target.Resolve(spec.Features))
	record.GeneratorCommit = commit
	record.FeatureModules = spec.FeatureModules
	record.Languages = spec.Languages
	record.MinSDK = spec.MinSDK
	record.TargetSDK = spec.TargetSDK
	record.FontName = spec.FontName
	record.AccentColour = spec.AccentColour
	record.Spec = spec.Sanitised
	record.Architecture = cmp.Or(spec.Architecture, target.Defaults.Architecture)
	record.DesignStyle = cmp.Or(spec.DesignStyle, target.Defaults.DesignStyle)
	record.MotionStyle = cmp.Or(spec.MotionStyle, target.Defaults.MotionStyle)
}

// setFeatures records a resolved feature set with the preset nearest to it and the differences.
func setFeatures(record *store.Generation, target *generate.TargetEntry, resolved []string) {
	record.Features = resolved
	record.Preset, record.Added, record.Removed = target.NearestPreset(resolved)
	record.Glass = slices.Contains(resolved, glassFeature)
}

// refuseTarget answers a target this deployment does not generate.
func (s *Server) refuseTarget(w http.ResponseWriter, target string) {
	if entry := s.catalogue.Target(target); entry != nil {
		writeError(w, http.StatusNotFound, "target_unavailable",
			fmt.Sprintf("%s projects cannot be generated here yet.", entry.Title), "")
		return
	}
	writeError(w, http.StatusNotFound, "unknown_target", "There is no such platform.", "")
}

// identifiers keeps the names that are safe to put in a response header.
func identifiers(names []string) []string {
	var safe []string
	for _, name := range names {
		if name != "" && strings.Trim(name, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-") == "" {
			safe = append(safe, name)
		}
	}
	return safe
}

func (s *Server) geo(r *http.Request) telemetry.Geo {
	if !s.cfg.TrustedProxy.Vercel {
		return telemetry.Geo{}
	}
	return telemetry.ReadGeo(r.Header)
}
