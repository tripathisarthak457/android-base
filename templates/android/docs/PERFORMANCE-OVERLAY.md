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
| Frame times | The last frames as bars against the 60, 90 and 120 Hz budgets: the full frame faded, its main-thread time solid. Red missed its deadline; amber made it with the main thread over the budget. |
| Frames per second | One bar for each of the last 60 seconds of drawing, against the display's rate. |
| Main-thread time histogram | Every frame recorded, by main-thread time, with one refresh interval at the display's rate while drawing as a line. |
| Janky frames by screen | Which destination the janky frames were drawn on, worst first. |
| Memory | Java and native heap each second, PSS at most every 15 seconds and only in a pause in the drawing, garbage collections. |
| Start-up | Cold, warm and hot start as the system timed them (Android 15 and later). |
| Network | Request timings from the inspector's request log, overall and per endpoint. |
| StrictMode | Disk and network calls the main thread made while recording, with where. |
| Build | Whether R8 ran, whether ART compiled the baseline profile, whether it is debuggable. |

## What it costs, and why it is safe to leave on

The overlay is built so that watching does not change what is watched:

- **No listener while off.** With Live stats off, no frame listener is on the window and nothing
  is sampled; the recording buffers are allocated when recording first starts, or when the Perf
  tab first reads them. Off is the default in `devRelease` and
  `devBenchmarkRelease`, the builds the perf gates measure. The recorder itself is built at launch
  with the inspector, so that a launch extra or a stored choice can turn it on from the first
  screen; measured, a hidden `devRelease` cold start was within noise of the same app without the
  overlay (517 against 507 ms median over 30 launches each).
- **One listener while on.** Frames come from JankStats. With the `analytics` feature, the overlay
  shares the analytics recorder's JankStats instead of adding a second.
- **Nothing per frame on the main thread.** JankStats hands each frame over on its own thread; the
  overlay copies a few numbers into fixed-size arrays and returns. No allocation, no boxing and no
  log line per frame.
- **No frame of its own.** The two lines under the badge are drawn straight from the recording,
  with no state and no recomposition, and redrawn at most four times a second from the window's
  pre-draw pass, so only inside a frame the app draws anyway. A redraw on a timer would cost a
  frame each time while the app is idle, and on a phone a lone frame after idle usually misses its
  deadline: the overlay would make the jank it reports. A redraw allocates nothing either: the
  numbers are written into reused buffers and drawn with one paint, with no string formatted and
  no text laid out inside the app's frame.
- **Graphs only on request.** The Perf tab draws its graphs only while it is open.
- **PSS only in a pause.** Reading PSS walks every memory mapping of the process under the kernel's
  lock on its address space, and on a phone a read during a scroll costs about one janky frame. The
  overlay reads it at most every 15 seconds, and only when no frame has started for a quarter of a
  second; an app that draws without a pause gets its PSS read late rather than its frames delayed.
  The same goes for your own code: never call `Debug.getMemoryInfo` while the app is animating.

Measured on a Redmi Note 10 Pro Max (M2101K6I) at 120 Hz with `devRelease`, alternating launches with the overlay
hidden and shown, each scrolling a list, switching every tab and pushing and popping a screen,
`dumpsys gfxinfo` read around every step. Without `analytics`, where the overlay brings the only
JankStats, 13,770 frames hidden and 13,764 shown were 1.34 % and 1.43 % janky, and the main thread
spent 0.09 ms more per frame shown. With `analytics`, whose JankStats it shares, 14,126 frames
hidden and 14,113 shown were 1.46 % and 1.41 % janky, with no change in main-thread time per
frame (+0.003 ms). The budget the overlay is held to is 0.3 percentage points of janky frames and
0.5 ms per frame.

**Measure the app with the overlay off.** The perf gates, `tools/perf/perf.sh` and any number you
quote are taken hidden. Your choice of Live stats is kept across launches, and across reinstalls
that keep the app's data, so a run after you once turned it on would measure the overlay too:
launch gate runs with `--es perf.hud off` (`perf.sh --extra perf.hud=off`), or `adb shell pm clear`
the app first.

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
yet (no start record, no PSS read) counts for nothing either way. There is no score at all before
300 frames are recorded: the frames are what it is about, and the build checks alone would rate an
app nobody has used.

| Metric | Points | A, the target | Source |
|---|---|---|---|
| Janky frames | 25 | at most 0.5 % | `tools/perf/gates.toml` scroll rows; the target is about 0 % |
| Main-thread time p90 | 10 | at most 0.6 of the budget | each frame's own refresh interval; `gates.toml` frame times are UI-thread time too |
| Main-thread time p99 | 10 | at most the budget | |
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

### Main-thread percentiles

Each frame's main-thread time (input, animation, composition, layout, draw and sync) against its
own refresh interval, 8.3 ms at 120 Hz and 16.7 ms at 60: the time the main thread had before the
next frame started. A display with an adaptive rate draws a scroll at 120 Hz and a lone frame after
idle at 60, and each frame is held to the deadline it had. The p90 says how heavy ordinary frames
are, the p99 how heavy the worst ones are. A frame's full
duration, from its vsync to the GPU finishing, is not graded against one interval: the render
pipeline gives a frame more time than that, so frames that make their deadline routinely run past
one interval. Whether a frame made its deadline is **Janky frames**.

A high p90 is usually layout work done twice (intrinsics, nested weights), lists without
`contentType`, or work in composition that belongs in `remember` or the draw phase. A high p99 is
usually first frames: a screen built for the first time, a list item type seen once.

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
code on the stack. Android 9 and later. The checks are added to your own policy, penalties
included, and your policy is put back when recording stops. Three consequences: a policy with
`penaltyDeath` also dies of the added checks while recording; a `penaltyListener` of your own is
not called while recording, since a policy holds one listener and the platform offers no way to
read yours to pass violations on; and a policy you set while recording replaces the overlay's
checks until recording starts again.

### Build checks

R8 shrinks and optimises the code; a baseline profile lets ART compile the start-up and first-use
code ahead of time; a debuggable build turns most compilation off. All three hold for a release and
fail for a debug build. A profile **waiting for compilation** is installed and compiles at the next
idle maintenance; `adb shell cmd package compile -f -m speed-profile <applicationId>` does it now.

## The log

One logcat tag, `PerfHud`, a line for every five seconds in which the app drew and a line for each
screen it leaves. Never a line per frame. Lines are `key=value` pairs. A window line's counts,
garbage collections included, are that window's, and its memory sizes the latest sample's; a
screen line sums every visit to that screen since recording began. `frameP50`, `frameP90` and
`frameP99` are percentiles of each frame's full duration, from its vsync to the GPU finishing,
which frames on time routinely run past one interval; `uiP50`, `uiP90` and `uiP99` of its
main-thread time, the part [the score](#main-thread-percentiles) grades. Times are in
milliseconds:

```
PerfHud: window=5s frames=598 fps=119.6 janky=0.2% frameP50=5.1 frameP90=6.8 frameP99=9.9 uiP50=1.2 uiP90=1.6 uiP99=2.9 slow=0 frozen=0 long=0 heapMb=21.4 nativeMb=17.0 gc=1 screen=SampleListKey
PerfHud: screen=SampleDetailKey frames=412 janky=0.5% frameP50=5.0 frameP90=7.2 frameP99=12.4 worst=19.8 slow=1 frozen=0
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

The receiver is declared in the debug manifest only, and not exported: no other app can reach it,
and adb reaches it only through `run-as`, which works on a debuggable build alone. `--user 0` is the
app's own user (on a phone with several users, the one it runs as): `run-as` cannot broadcast to the
default, "current", which needs a cross-user permission. A release-like dev build exports from the
tab.

A launch with `--es perf.hud on` (or `off`) switches recording for that process, whatever the
build, which is how `tools/perf/perf.sh` measures the overlay's own cost (`--extra perf.hud=on`).
It does nothing else, and nothing in a shippable release reads it.

The JSON has a `schema` number that changes when a field is renamed or removed.

## Turning it off

The overlay is the generator's `perfhud` feature. To remove it from a project, delete
`core/perfhud`, its `include` in `settings.gradle.kts`, the two `:core:perfhud` lines in
`app/build.gradle.kts` and its entry in `build-logic`'s `DevToolingGuard.kt`.
