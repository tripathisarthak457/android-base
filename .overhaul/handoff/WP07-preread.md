# WP07 pre-read (package not started)

## Progress

Not started. Notes for the agent that starts WP07; copy them into `handoff/WP07.md` when it is created.

## Orchestrator note: motion-perf

Written 2026-10-09. Read `research/SMOOTHNESS-PLAN.md` (S1, S3, S4, S8–S11), `research/MOTION-PLAN.md` (§1 tokens, M1,
M3, M10, M11, M20, M24), `research/RECOMPOSITION-PLAN.md` (§2, §3, step 7) and `research/predictive-back/SPEC.md` §8.
All MISSING (package not started). The Compose Multiplatform template must reach the same bar with the same tokens.

1. **Shared tokens.** `AppSprings` (`screen`, `thumb`, `dialog`, `interactive`, `bounceBack`, `indicatorLead/Trail`,
   `fling`) live in `commonMain` with the same values as Android.
2. **The same structures in common code**: the persistent stack scene with parked entries (S1); the clock after
   composition (S3); parked kept tabs with no flips on the switch frame (S4); drawn scrims (S8); `Modifier.Node`
   motion; the pull-to-refresh node (M20); the overscroll rubber band (M10); the tab reveal and two-edge indicator
   (M24).
3. **Android target**: the Gmail card from SPEC unchanged (navigationevent is multiplatform).
4. **iOS target**: there is no system back gesture, so build an interactive edge-swipe pop (M3): commit at ≥ 1 W/s or
   past 50 %, on `screen` with the velocity. iOS deceleration (M11) is essential there. Check what Compose
   Multiplatform's iOS scrolling already does [U].
5. **Gates.**
   - The stability config and the compiler-report gate in the shared module (RECOMPOSITION §2).
   - The `RecompositionCounter` harness in `commonTest` through `runComposeUiTest` (`Composer.setTracer` is in
     `commonMain`), on desktop/JVM. The iOS simulator is [U].

## Orchestrator note: adaptive-standards

Written 2026-10-09. Read `research/ADAPTIVE-PLAN.md` (§0, §3, §13) and `research/STANDARDS-PLAN.md` (§2.11, §4).
All MISSING (package not started).

1. **AD-50: adaptive parity** (ADAPTIVE-PLAN §13).
   - `foundation/adaptive/` goes to `commonMain` byte-identical, as a `sync.toml` pair. `window-core` 1.5.1 has iOS
     artifacts.
   - The class gating is **mandatory**: CMP-iOS lerps `containerSize` every frame of a rotation.
   - `WindowPosture` and `InputTraits` are `expect`/`actual` (iOS: `Flat`; `GCKeyboard` for the hardware keyboard).
   - Panes: check whether `org.jetbrains.compose.material3.adaptive:adaptive-navigation3` exists for iOS at a version
     built against JetBrains `navigation3-ui` 1.1.2. If not, port `PaneSceneStrategy` using only
     `Scene`/`SceneStrategy`.
   - `ShellChromePolicy` per platform (iPhone keeps the bottom bar held sideways).
   - iPad `Info.plist`: four orientations, no `UIRequiresFullScreen`, `UILaunchScreen`,
     `UIApplicationSupportsIndirectInputEvents`, single scene. `test_target_multiplatform.py` asserts them.
   - The right-click fix in shared code.
   - Roborazzi `androidHostTest` and `ResizeContinuityTest` A, B and C.
   - Never `MediaQuery`: it has no iOS implementation and throws.
2. **Structure** (STANDARDS-PLAN §2.11):
   - `commonMain` mirrors the Android packages;
   - actuals are `X.android.kt` and `X.ios.kt` in the same package;
   - `commonTest` names are camelCase (K-8);
   - `sync.toml` lists the new `foundation/<sub>/` and `core/navigation` sub-package paths.
3. **Vendored skills** (STANDARDS-PLAN §4.1–4.4): `templates/multiplatform/.agents/skills/` holds the Android set
   minus `agp-9-upgrade`, copied by WP02-M2's `tools/update_android_skills.py`, as a `sync.toml` pair. WP12 writes the
   multiplatform AGENTS.md rows.
