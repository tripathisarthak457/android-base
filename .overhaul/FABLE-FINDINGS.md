# Fable findings (Oct 15, 2026; written 20:52 from the two Fable handoffs, updated Oct 16 10:40)

Fable 5.1 started two deep-work workflows on Oct 15. Full detail is in their handoffs; this note keeps what matters
for the release and for anyone resuming.

**Model change (the user, Oct 15 20:55 and Oct 16):** Fable used too much of the usage limit, so it is no longer
used. Both workflows continue **on Opus 5.5** with the same goals: the same scripts (still named `*-fable.js`), the
same branches, resuming from these handoffs. Every Fable option was removed from the scripts, so nothing can run on
Fable again. Fable's weekly usage stopped at 55%.

| Workflow | Branch / worktree | Handoff | Phone data |
|---|---|---|---|
| Deep smoothness (`scripts/deep-smoothness-fable.js`, now on Opus) | `wp/WP04-perf`, `wt/WP04-perf` | `handoff/WP04-perf.md` | `perf/WP04-perf/` (`dvfs/`, `everything/`) |
| Full liquid glass, REQUIREMENTS 37 (`scripts/full-liquid-glass-fable.js`, now on Opus) | `wp/WP05-glass`, `wt/WP05-glass` | `handoff/WP05-glass.md` | `perf/WP05-glass/` |

## 1. Smoothness: the remaining jank is the first frame after idle

Measured with Perfetto (FrameTimeline, cpufreq, sched) on the Redmi Note 10 Pro (Snapdragon 732G, Adreno 618,
120 Hz).

| Stage | Steady frame | First frame after idle |
|---|---|---|
| UI thread (compose, layout, record) | 1.8 ms | 6–11 ms on a big core at its 652 MHz floor; 32–34 ms if it lands on a little core |
| RenderThread | 2.7 ms | 3–10 ms |
| GPU | 5.5 ms | 9–15 ms (still at its idle clock) |
| **Total** | **9.5 ms** | **17–29 ms** against a 13.67 ms deadline |

- **Cause:** power saving. Every transition starts with the CPU and GPU at their lowest clocks. This ROM's touch boost
  raises only the little cluster (576 → 1248 MHz) and leaves the big cluster at 652 MHz.
- **Correction to the earlier plan:** performance hints (ADPF) will not fix it. HWUI already runs an ADPF hint
  session and sends `sendCpuLoadResetHint` every frame, and the clocks still do not move. No app API raises them on
  this phone. **The only lever is making the first frame cheaper**, which is what the fixes below do.
- **What the numbers mean:**
  - The tab-switch janky % is exactly one frame per switch (1/87 = 1.15%). The push/pop p99 and the gesture janky %
    are the same frames, plus the frames queued behind them.
  - SurfaceFlinger does not blame the app for these frames (`Non Animating / Unspecified Present`), but HWUI's
    deadline check, and so gfxinfo, counts them.
- **Scroll hitches** (14–15 ms mid-fling) happen when the scheduler moves the RenderThread to a little core for one
  frame: RenderThread 7–8 ms instead of 3.1 ms for the same display list.
- **GPU frequency cannot be read on this phone** (`/sys/class/kgsl` is denied to the shell and to traced_probes).

### Waste found in the first frame, and the fixes (merged into `wp/WP04` at `730f8e0`)

| Problem | Fix | Commit |
|---|---|---|
| A pop's first frame recomposed the whole leaving screen (416 composables): once off the stack it got a new pane context, and `LocalPaneContext` is a static local | A leaving entry keeps its pane context (`StackLayout.kt`) | `6436b75` |
| Every tab switch recomposed all ten bar cells: the tap lambda captured `selected` | The tap handler reads the selection through state (`AppTabBar.kt`); only the two cells that flip recompose | `1ec7462` |
| The selection pill was a path of cubics, which Skia cannot clip analytically | Build it as a rounded rect (`TabCells.kt`) | `b7e8074` |
| `AppScaffold` painted the background over `EntryFrame`'s on every frame: a second full-screen fill (2.6 Mpx), about a third of a frame's GPU time | The host says the background is painted (`LocalPaintedBackground`); one fill per frame | `ac56e69` |

Tests: `ShellRecompositionTest` checks that a pop composes nothing of the leaving screen and that a switch recomposes at
most two cells. 34 navigation tests pass.

### Still in progress (round 2; carried on by Opus)

- **Uncommitted, tests running:**
  - `KeptTabs.kt`: the hidden tab's lifecycle change moves to the frame after the switch.
  - Sample detail: the related rows wait for `LocalNavigationSettled`. They had composed 185 composables 220 ms into
    every push.
- **Correction (Oct 19): the Oct 16 phone measurement did not finish.** `out/WP04-perf/logs/measure2.log` shows the
  HEAD build ran two of its three runs, then `perf.sh` exited 2 when uiautomator could not dump the screen; fixed and
  floor were never measured. Every build was rebuilt from its commit and measured on Windows on Oct 18
  (`handoff/WP04-perf.md` §0 and §3: the fixed build's first frame is within 1–3 ms of the floor's, which misses the
  deadline on 7 of 7 switches). The plan it was part of compares three builds:
  - **HEAD** (before the fixes);
  - **fixed**;
  - **floor**: the fixed build with every screen's content removed. This shows the irreducible cost of a switch: one
    fill, the bar and the fade.

  If the fixed build is close to the floor, the remaining first-frame jank is the phone's power policy, not our
  code. That becomes a documented device limit, not a code fix.

## 2. Liquid glass: playground parity, bottom bar first

### What the Kyant playground does (read from its source, not the APK)

- **Source:** `github.com/Kyant0/AndroidLiquidGlass`, branch `kmp`. Saved under `research/glass/kyant-catalog/`
  (playground) and `research/glass/kyant-src/` (library 2.0.1).
- **Bottom bar (`LiquidBottomTabs`):** three stacked layers, all driven by one damped drag animation.
  - **Bar body:** `vibrancy; blur(8); lens(24, 24)`, a rim highlight, a 24 dp shadow, a 0.4 tint, and a press swell
    of `1 + 16/width`.
  - **Tabs copy:** an invisible, accent-tinted copy of the tabs, sampled so the lens can show it magnified.
  - **The lens:** `lens(10·p, 14·p)` with chromatic aberration, a highlight, an inner and an outer shadow, a swell to
    1.39×, and a stretch along the motion that follows the velocity.
  - **A tap** is the same morph as a drag's release: it presses, travels on `spring(1, 1000)`, and releases.
  - **Also:** a press glow at the finger, and the whole bar gives 4 dp with a drag.
- **Also mapped:** `LiquidButton`, `LiquidToggle`, `LiquidSlider`, the dialog, the glass playground and the
  control-center tiles, with exact values (handoff §1.2–1.6).
- **Why the playground is expensive (PeriFlow's G1 mistake):** on every frame of a tap or drag it:
  - rebuilds two RenderEffect chains;
  - re-records the bar's sample;
  - samples the bar a second time for the tabs copy;
  - re-records three highlight and shadow layers, one of them bar-sized plus 96 dp with a blur.

  The dispersion shader also reads every pixel seven times. **Rule for us: keep every visible effect, but don't
  rebuild anything per frame.**

### What `wp/WP05-glass` builds (in progress; 39 changed or new files, uncommitted)

The new `LiquidTabs.kt` is the bar's `Glass` treatment, chosen by a new `Glass` design style that needs the
liquidglass feature. Each playground effect gets a cheap equivalent:

| Effect | Playground | Ours | Why it costs less |
|---|---|---|---|
| Bar blur and vibrancy | `vibrancy; blur(8)` | the same | the scene is recorded once and shared |
| Rim refraction | `lens(24, 24)` | the same, on the Live tier | a fixed effect chain |
| Rim light | a highlight node, re-stroked every draw | `RimLight.kt`, the same maths | recorded once per size |
| Shadows | re-recorded every draw | Compose's cached shadow (light theme) | drawn once per size |
| Press swell, drag give | layer scale and offset | the same (`BarSwell`, `BarGive`) | nothing is re-recorded |
| Press glow | a shader every frame | `PressGlow.kt`, recorded once and moved | no shader or allocation per frame |
| Tabs seen through the lens | a second full-bar sampling pass | WP04's selected-tab layer reused as a backdrop | no second composition |
| Lens refraction and dispersion | rebuilt with the press | a fixed `lens(12, 20)` with dispersion; the intensity drives the tint and the zoom | one chain, never rebuilt |
| Lens highlight, inner shadow, cast shadow | allocated every frame | layer alpha, one gradient brush, a cached shadow | alpha changes only |
| Lens swell and velocity stretch | 1.39×; velocity-based | 1.39×; read from the spring's energy | the stretch holds through a turnaround |
| Tap | press, travel, release | the travelling lens (REQUIREMENTS 35) | only the lens reads it |
| Drag | on the lens only | hold or drag anywhere on the row; the release projects the velocity and ticks for each tab crossed | one pointer handler, no coroutine per event |
| First use | — | the shaders compile at launch (two warm-up frames) | no stall on the first tap |

- **Tiers:**
  - **Live** has everything.
  - **LiveNoLens** (API 31–32, or the phone running warm) keeps the blur, vibrancy and rim light, without
    refraction or the lens.
  - **Frosted** samples nothing.
  - **Solid** is the style's plain bar.
  - **Reduced motion** keeps the lens plain.
- **Measurement:** the perf runner checks the `perf.glass` tier line in the log before it keeps a forced tier's
  numbers.
- **A useful preview of a future merge:** to build against both packages, Fable merged WP04 and WP03 into this branch.
  WP03 conflicted in 21 files; the resolution is in `282dd3c`, with the decisions in handoff §2.0 and §2.1. The real
  merge of WP03 after WP04 will hit the same conflicts; reuse that resolution.
- **Not done yet:**
  - the other components (button, toggle, slider, dialog, sheet);
  - phone recordings side by side with the playground;
  - perf gates on the phone;
  - the review.

  The Glass sheet style waits for WP03's real sheet (`SheetSurfaceNode`); the branch has only a catalog demo.

## 3. What this changes in the plan

1. **Smoothness:**
   - Drop "ADPF hints at interaction start" from the plan.
   - Once round 2's measurements are in, decide whether the remaining first-frame jank is a device limit, and record
     it in the release gate with the floor build's numbers.
2. **WP04** has merged the round-1 fixes. Round 2 (`KeptTabs`, sample detail) merges the same way after its tests
   and review.
3. **WP03's merge after WP04:** use `282dd3c` on `wp/WP05-glass` as the reference resolution.
4. **WP05** merges `wp/WP05-glass` when the glass work is reviewed and passes its phone gates. Only then can CP2 and
   REQUIREMENTS 37 be marked done.

## 4. Status, Oct 16 10:40

| Workflow | Task (Opus) | Next |
|---|---|---|
| Deep smoothness | `wld2x35jp` / `wf_18d8ae1b-418` | Analyse round 2 (HEAD, fixed, floor); commit `KeptTabs` and the sample-detail change after their tests; run the new Compose performance skill's audit over the shell; then the review loop. WP04 merges the result |
| Full liquid glass | `wklpx52e0` / `wf_c9329bf3-67e` | Commit the bar work (43 files changed or new); button, toggle, slider, dialog and sheet; side-by-side phone strips against the playground; `glass_live` gates; review. WP05 merges the result, then CP2 |
