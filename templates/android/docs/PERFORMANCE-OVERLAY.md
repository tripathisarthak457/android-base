# The performance overlay

Dev and debug builds carry a live performance overlay beside the DEV badge, and a **Perf** tab in
the inspector the badge opens. It answers one question while you work: does the app still draw
every frame on time? A production build does not carry it at all: `:core:perfhud` is a dependency
of debug builds and of the dev flavour only, so `prodRelease` and `playstoreRelease` hold none of
its code.

## Reading it

The two lines under the badge are:

- **`118/120 fps`**: frames per second over the last second of drawing, against the display's
  current rate. Too little drawing to judge (a lone frame, a blinking cursor) reads **idle** rather
  than a low number: Android draws only when something changes, and not drawing is correct.
- **`0.4% janky`**: the share of every frame recorded so far that missed its deadline.

They refresh up to four times a second while the app draws, and only then: while it is idle they
keep the numbers of its last drawing (see [What it costs](#what-it-costs-and-why-it-is-safe-to-leave-on)).

In a release-like build they are green when on target, amber when close, red when not. In a debug
build they stay one colour, because a debug build's frames say little about the release's (see
[Debug builds](#debug-builds)).

Tap them to open the Perf tab. It shows, from the top:

| Part | What it is |
|---|---|
| Live stats | The switch. Off, nothing is recorded and nothing listens to the window. |
| Export report | Writes a JSON and a Markdown report (see [Reports](#reports)). |
| Optimisation score | 0 to 100, each lost point with its fix (see [The score](#the-score)). |
| Frame times | The last frames as bars against the 60, 90 and 120 Hz budgets. Red missed its deadline. |
| Frames per second | One bar for each of the last 60 seconds of drawing, against the display's rate. |
| Frame-time histogram | Every frame recorded, by duration, with the current budget as a line. |
| Janky frames by screen | Which destination the janky frames were drawn on, worst first. |
| Memory | Java and native heap each second, PSS every 15 seconds, garbage collections. |
| Start-up | Cold, warm and hot start as the system timed them (Android 15 and later). |
| Network | Request timings from the inspector's request log, overall and per endpoint. |
| StrictMode | Disk and network calls the main thread made while recording, with where. |
| Build | Whether R8 ran, whether ART compiled the baseline profile, whether it is debuggable. |

## What it costs, and why it is safe to leave on

The overlay is built so that watching does not change what is watched:

- **No listener while off.** With Live stats off, no frame listener is on the window and nothing is
  sampled or allocated. Off is the default in `devRelease` and `devBenchmarkRelease`, the builds the
  perf gates measure.
- **One listener while on.** Frames come from JankStats. With the `analytics` feature, the overlay
  shares the analytics recorder's JankStats instead of adding a second.
- **Nothing per frame on the main thread.** JankStats hands each frame over on its own thread; the
  overlay copies a few numbers into fixed-size arrays and returns. No allocation, no boxing and no
  log line per frame.
- **No frame of its own.** The two lines under the badge are drawn straight from the recording,
  with no state and no recomposition, and redrawn at most four times a second from the window's
  pre-draw pass, so only inside a frame the app draws anyway. A redraw on a timer would cost a
  frame each time while the app is idle, and on a phone a lone frame after idle usually misses its
  deadline: the overlay would make the jank it reports.
- **Graphs only on request.** The Perf tab draws its graphs only while it is open.

Measured on a Redmi Note 10 Pro at 120 Hz with `devRelease` (the numbers are in the overhaul's
`perf/HUD/`): the overlay shown adds at most 0.3 percentage points of janky frames and under
0.5 ms per frame against the overlay hidden.

## Debug builds

A debug build runs without R8, without a baseline profile, and with most of its code interpreted by
ART, so it drops several times more frames than the same code in a release. The overlay says so in
the tab and in its log, scores the build checks as failed, and draws its numbers without a verdict
colour. For numbers that mean something:

```bash
./gradlew :app:installDevRelease
```

## The score

Each metric gets a grade from A to F against the template's own targets, and the score is the
points earned out of the points of the metrics that were measured. A metric with nothing to measure
yet (no start record, fewer than 300 frames) counts for nothing either way.

| Metric | Points | A, the target | Source |
|---|---|---|---|
| Janky frames | 25 | at most 0.5 % | `tools/perf/gates.toml` scroll rows; the target is about 0 % |
| Frame time p90 | 10 | at most 0.6 of the budget | the budget at the display's rate |
| Frame time p99 | 10 | at most the budget | |
| Slow frames (over 16 ms) | 5 | at most 0.5 % | Android vitals |
| Frozen frames (over 700 ms) | 10 | none | Android vitals |
| Long main-thread work (over 50 ms) | 5 | none | |
| Cold start | 10 | at most 380 ms | `gates.toml` `coldstart` |
| Warm start | 5 | at most 190 ms | half the cold-start gate |
| App memory, PSS less graphics | 10 | at most 48 MB | `gates.toml` `meminfo.pss_app_mb` |
| Blocking garbage collections | 5 | none | |
| StrictMode violations | 5 | none | |
| R8, baseline profile, not debuggable | 5 each | pass | |

### Janky frames

A frame is janky when it missed its deadline: the platform's FrameTimeline count on Android 12 and
later, the same one `perf.sh` and `dumpsys gfxinfo` report. Start with the worst screen in
**Janky frames by screen**, then look for, in this order: recomposition on every frame of an
animation (read animated values in `graphicsLayer`, `offset { }` or `drawBehind` lambdas), alpha on
a full-screen layer, main-thread work started by the navigation itself, and a first frame that
builds a whole screen. A trace from the android-profiler skill names the cause.

### Frame time percentiles

The p90 says how heavy ordinary frames are, the p99 how heavy the worst ones are. A high p90 is
usually layout work done twice (intrinsics, nested weights), lists without `contentType`, or work
in composition that belongs in `remember` or the draw phase. A high p99 is usually first frames: a
screen built for the first time, a shader compiled on first use, a list item type seen once.

### Slow and frozen frames

Android vitals' thresholds: a slow frame took over 16 ms, which misses even a 60 Hz budget; a
frozen one over 700 ms, which the person saw as the app hanging. A frozen frame almost always means
I/O, a large decode or a lock on the main thread: check **StrictMode**.

### Long main-thread work

A frame whose main-thread part took over 50 ms. StrictMode names disk and network calls; a trace
names the rest. Defer what the first frame does not need.

### Start-up

Read from Android 15's `ApplicationStartInfo`, which the system fills: launch to first frame (time
to initial display) and launch to `reportFullyDrawn` (time to full display, when the app reports
it). Earlier releases have no such record. Keep `Application.onCreate` and the first screen free of
I/O and eager singletons, and measure with `tools/perf/perf.sh coldstart` on `devRelease`.

### Memory

PSS less graphics is the app's own memory: the rest is the window's buffers, about 50 MB on a
1080p phone whatever the app does. Blocking garbage collections stop every thread and come from
allocating fast, typically objects created on every frame.

### StrictMode

While recording, StrictMode watches the main thread for disk reads and writes, network, unbuffered
I/O, resource mismatches and calls marked slow, and counts them with the first frame of your own
code on the stack. The app's own policy is put back when recording stops. Android 9 and later.

### Build checks

R8 shrinks and optimises the code; a baseline profile lets ART compile the start-up and first-use
code ahead of time; a debuggable build turns most compilation off. All three hold for a release and
fail for a debug build. A profile **waiting for compilation** is installed and compiles at the next
idle maintenance; `adb shell cmd package compile -f -m speed-profile <applicationId>` does it now.

## The log

One logcat tag, `PerfHud`, a line for every five seconds in which the app drew and a line for each
screen it leaves. Never a line per frame. Lines are `key=value` pairs:

```
PerfHud: window=5s frames=598 fps=119.6 janky=0.2% p50=5.1 p90=6.8 p99=9.9 slow=0 frozen=0 long=0 heapMb=21.4 nativeMb=17.0 gc=1 screen=SampleListKey
PerfHud: screen=SampleDetailKey frames=412 janky=0.5% p50=5.0 p90=7.2 p99=12.4 worst=19.8 slow=1 frozen=0
```

```bash
adb logcat -s PerfHud
```

## Reports

**Export report** in the tab writes `perf-report-<time>.json` and `.md` to the app's external files
directory. adb can read them from a release build too:

```bash
adb pull /sdcard/Android/data/<applicationId>/files/perfhud/
```

On a debug build, the same from a script, without touching the phone:

```bash
# Turn recording on or off, then export; the broadcast's result data is the JSON's path.
adb shell run-as <applicationId> am broadcast --user 0 \
    -n <applicationId>/com.base.app.core.perfhud.report.ReportReceiver \
    -a com.base.app.core.perfhud.action.RECORD --ez recording true
adb shell run-as <applicationId> am broadcast --user 0 \
    -n <applicationId>/com.base.app.core.perfhud.report.ReportReceiver \
    -a com.base.app.core.perfhud.action.EXPORT
```

The receiver is not exported: no other app can reach it, and adb reaches it only through `run-as`,
which works on a debuggable build alone. `--user 0` is the app's own user (on a phone with several
users, the one it runs as): `run-as` cannot broadcast to the default, "current", which needs a
cross-user permission. A release-like dev build exports from the tab.

A launch with `--es perf.hud on` (or `off`) switches recording for that process, whatever the
build, which is how `tools/perf/perf.sh` measures the overlay's own cost (`--extra perf.hud=on`).
It does nothing else, and nothing in a shippable release reads it.

The JSON has a `schema` number that changes when a field is renamed or removed.

## Turning it off

The overlay is the generator's `perfhud` feature. To remove it from a project, delete
`core/perfhud`, its `include` in `settings.gradle.kts`, the two `:core:perfhud` lines in
`app/build.gradle.kts` and its entry in `build-logic`'s `DevToolingGuard.kt`.
