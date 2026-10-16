## Smoothness, motion and adaptive UI (enforced)

<!-- Draft for WP12. Each rule is sourced in SMOOTHNESS.md §2/§4, MOTION.md §1, ADAPTIVE.md; verify names against merged code. -->

Target: iOS-level smoothness on a 120 Hz mid-range phone (8.33 ms per frame): ~0 % janky frames on tab switches,
push/pop, back and scroll, and no dropped first frames. Smoothness never costs animation: make an effect cheap, never
remove it. Full guides: [docs/SMOOTHNESS.md](docs/SMOOTHNESS.md) (root causes, numbers, gates),
[docs/MOTION.md](docs/MOTION.md) (springs, predictive back, tab switch), [docs/ADAPTIVE.md](docs/ADAPTIVE.md).

**Rules**
1. Read animated, dragged or scrolled values only in `offset {}`, `layout {}`, `graphicsLayer {}`, draw blocks or a
   `Modifier.Node`. A component taking a fast-changing value takes `() -> T`.
2. No alpha on full-screen layers. Scrims are `drawBehind { drawRect(scrim, alpha = …) }`; card and row fades use
   `CompositingStrategy.ModulateAlpha`. Screens slide; they never fade or scale.
3. A screen's first composition is final: seed state from the caller or `peek(id)`; load secondary sections after
   `LocalNavigationSettled` turns true (read it in an effect). Never swap skeleton → content during a transition.
4. Never touch a back stack or `NavDisplay` from a feature: register `entry<Key>(…)` and navigate via `AppNavigator`.
5. Every scrolling container pads with `chromeContentPadding(base)` in `contentPadding`; never parent padding, never a
   strip behind the bar. Content scrolls under the floating bar.
6. Springs come from `AppTheme.springs`. Every gesture ends on a spring seeded with the finger's velocity; never a tween.
7. Every control states its feedback: `Modifier.pressable(onClick, feedback = …)`; `selectable`/`toggleable` take
   `indication = null`. No ripple, no bare `.clickable(`.
8. Gesture state lives in state read at placement; never use values captured when the gesture started.
9. Lazy lists: stable `key` on every item; `contentType` on mixed lists.
10. No allocation in per-frame draw code: build brushes, paths and shapes in `drawWithCache`.
11. Nothing animates while idle or hidden: infinite animations stop when the tab is hidden; idle renders 0 frames.
12. Never change a CompositionLocal value or a lifecycle owner on a transition frame; changing values go through a
    static local holding a `@Stable` holder.
13. Adaptive: layout decisions read `AppTheme.containerClass` (screens, components) or `AppTheme.windowClass` (shell
    only), never orientation, device type or `Configuration.screenWidthDp`. Never lock orientation or resizability.
    Multi-pane layouts are declared with `entry(pane = …)`, never a hand-built `Row`. Every `XScreen` has an
    `@AppScreenPreviews` preview.
14. Glass (when on): never fade or hide glass chrome with `AnimatedVisibility`; translate it. Only the lens samples.

**Forbidden** (each failed in this codebase; see SMOOTHNESS.md §2)
`Modifier.offset(y = animatedDp)` · `graphicsLayer { alpha = … }` on a full screen · `AnimatedVisibility` around
chrome · `if (selected) place(…)` to hide a tab · `tween(…)` after a drag · `LazyColumn(Modifier.padding(…))` for
insets · `.clickable {}` without stated feedback · a shimmer per element · `preloaded_fonts` meta-data ·
`screenOrientation` or `resizeableActivity="false"` · measuring a debug build.

**Pre-merge checklist (run before calling UI work done)**
- [ ] `./gradlew check` passes (Compose stability gate, lints `FrequentlyChangingValue` and
      `UseOfNonLambdaOffsetOverload`, code rules).
- [ ] A recomposition test covers each new screen or component: one journey, `assertQuietBetweenEnds()`;
      the budgets in SMOOTHNESS.md §6 hold.
- [ ] Light, dark, font scale 1.5 and 2.0, RTL: nothing clipped by the bar, title or status bar; no truncated labels.
- [ ] `@AppScreenPreviews` and the screenshot matrix pass; checked at compact, medium and expanded widths and a short
      landscape window; resizing within one class recomposes no screen root.
- [ ] Every new motion uses a token from MOTION.md and is interruptible; reduce motion behaves as MOTION.md §1 says.
- [ ] On a release build (`devBenchmarkRelease`, else `devRelease`) on a real phone, never debug or the emulator:
      `tools/perf/perf.sh --gate solid <scenarios>` passes for the journeys you touched (`tabs`, `pushpop`,
      `pushpop_gesture`, `back_during_push`, `rapid_back`, `scroll_root`, `idle`, `recompose`).
- [ ] If a journey changed: `./gradlew :app:generateBaselineProfile`, then `python3 tools/perf/split_profile.py`.
- [ ] Glass on: measured with `--extra glass.quality=Live` and unpinned.
- [ ] Recorded the motion (`tools/perf/record.sh`) and compared it with the reference in MOTION.md §8.
