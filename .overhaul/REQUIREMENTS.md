# Overhaul requirements (from the user, verbatim intent)

> **Every agent: also read `D:/projects/tools/.overhaul/notes/INDEX.md`.** It lists the plans
> written after the master plan: visual QA defects, smoothness, iOS motion, recomposition, adaptive UI, and the
> checkpoint decisions. The items that name your package are part of your acceptance.

Repo: D:/projects/tools/android-base (branch overhaul/multiplatform-v2).
The user owns it. Reference apps generated from it: PeriFlow (~/Documents/projects/jorwen/periflow/periflow-android),
VitFleet (~/Documents/projects/tools/vitfleet/vitfleet-android).

## Non-negotiable outcome
The generator must produce SMOOTH, LIGHT apps. Today generated apps are heavy, low-FPS and not smooth.
Every decision is judged against: does it keep 60/90/120 fps on a mid-range phone (Redmi Note 10 Pro, serial 9a7c74f4)?
If it runs smooth there, it runs smooth on most phones.

## Android generator
1. Fix ALL navigation glitches:
   - bottom-bar tab switches are "glitchy / pushy";
   - push/pop transitions glitch;
   - EXIT/BACK transition glitches when navigating back quickly (rapid back presses while a pop animation is still running) — reproduce on device with rapid `adb shell input keyevent KEYCODE_BACK`, fix the root cause, add a regression test.
2. Remove unnecessary ripple / press overlays. Press feedback must be iOS-like (subtle, spring-based, cheap), not a grey flash.
3. Liquid glass (Kyant backdrop, io.github.kyant0:backdrop) as an OPTIONAL feature for Android, optimised for FPS (PeriFlow's version dropped FPS — fix there too).
4. Components polished to Apple-software level; smoothest, optimised, iOS-like animations that do not hurt performance. Design more components; remove heavy ones nobody needs in practice.
5. Better floating bottom bars.
6. Best-in-class multi-module architecture: deeper directory structure, better organisation that can handle 1M-user, feature-heavy apps — without over-engineering small apps.
7. Remove AI-slop code (narrating comments, needless abstraction, dead code). Comments only where they explain WHY. It must not look AI-generated.
8. Fix the devtools API-log panel that cannot be dragged; find and fix all similar bugs.
9. Test edge cases and ALL types of build options, lean to full, all combinations.
10. Add practically useful generator features.

## New generators
11. Compose Multiplatform generator (Android + iOS targets), best architecture, same design-system quality.
12. SwiftUI native generator (SwiftUI + @Observable MVI, iOS 17 min, XcodeGen, local SPM packages, iOS 26 glass behind #available). Code complete but HIDDEN / "coming soon" on the web portal (no Mac to test).

## Web + API + docs
13. Update web portal, Python script, open-source repo docs with all changes. Platform picker: Android Native | Compose Multiplatform | Swift (coming soon).
14. Admin portal: much more detailed metrics of who generated what — richer ANONYMOUS telemetry only (geo region/city via Vercel headers, browser/OS/device class, referrer + UTM, funnel opened→configured→generated→downloaded, time in configurator, returning visitors, failure rates per feature combination, per-platform). No sign-in, no email.
15. Improve AGENTS.md, README, CLAUDE.md, docs/* and every file an AI agent reads — both in the generator repo and in every generated project — so agents build MORE POLISHED, SMOOTH apps from the generated project (motion/perf/design rules, checklists, do/don't with code).
16. Document the project perfectly.

## Process
- Port the good recent bits from PeriFlow and VitFleet back into the generator.
- Code review and fix bad code.
- Git: branch overhaul/multiplatform-v2; merge to main only when the full matrix is green (CI gates the Vercel deploy). PeriFlow fixes: local branch, committed, NOT pushed.
- Gradle builds and adb sessions are serialised through D:/projects/tools/.overhaul/bin/locked (gradle|adb). 16 GB RAM machine, Windows 11 since Oct 17 (native, no WSL).
- If the phone disconnects: there is no emulator on this machine. Do the code steps, poll for the phone (adb devices through bin/locked adb) for up to 45 minutes, then record which phone steps remain.
- **Memory safety (the laptop froze on Oct 10 at 15:23 under memory pressure).** `bin/locked gradle` runs one build machine-wide, waits for 3.5 GB of free memory (1.5 GB with a Gradle daemon resident) and caps Gradle workers at 4; the heap and the daemon idle time come from `D:/.caches/gradle/gradle.properties`. Run every other heavy command as `D:/projects/tools/.overhaul/bin/memwait <command...>`, with the command as its arguments (it also sets the SDK and cache variables the shell lacks): `npm ci`, `next build`, `go test`, Kotlin/Native/iOS klib compiles, ffmpeg batches. Never raise the Gradle heap.

## Added by the user on Oct 9, 08:40
17. **The bottom bar must not have an opaque white block around it** that cuts off the UI. Content scrolls under a floating bar, and only the scroll content is padded.
18. **Visual defect hunting is continuous.** While working, find more defects of this kind and fix them: opaque blocks, clipped content, truncated labels, overlaps, wrong insets, inconsistent spacing, poor dark mode, large-font breakage, RTL, tablet. Defects already seen in the new build:
    - 7 tabs crammed into one bar, with "Settin…" truncated (cap at 5 plus "More", or another sound rule).
    - Scaffolded modules all get the same icon (Orders and Wallet).
    - The selected tab's label overflows its indicator.
    - The bottom edge under the floating bar looks cluttered and needs an edge fade.
19. **Smoothness target: iOS level, the lowest achievable jank rate.** Research the best known techniques and apply them; the gates in MASTER-PLAN §2 are the floor, not the goal. On the Redmi at 120 Hz, aim for ~0% janky frames on tab switches, push/pop, back and scroll, with no dropped first frames.
20. **The final report must show before/after numbers** measured on the phone: old template vs new for the everything, standard and lean presets, and PeriFlow before vs after. Cover janky %, frame-time p50/p90/p99, GPU p90, first-frame UI time, cold start and PSS.
21. **Smoothness must not come at the expense of animation.** Include the full set of iOS-like animations and transitions, each implemented optimally:
    - push/pop with parallax and dim, and the interactive back swipe
    - zoom/hero transitions
    - modal sheets with detents and the card stack behind them
    - context menus with a preview
    - large-title collapse, and tab bar minimize on scroll
    - rubber-band overscroll, and scroll deceleration that matches iOS
    - spring press states, switch/segmented/picker motion, alert and action-sheet scale-in
    - list insert/delete/reorder, swipe actions, pull to refresh
    - search bar focus expansion, keyboard-synced motion, toast/HUD
    - glass morphing (when glass is on)

    Every animation is interruptible and keeps its velocity, runs in the layout/draw phase with no per-frame recomposition, and passes the perf gates.
22. **Optimise recomposition everywhere.** Stable parameters, deferred reads, no per-frame recomposition during animations, and Compose compiler metrics clean for the design system and the shell. Verify it with recomposition-count tests on the critical journeys.
23. **Adaptive UI everywhere, a best-in-class implementation.** Every component and every screen responds to the window size class, width and height:
    - all current breakpoints, including the large and extra-large widths
    - foldables: tabletop and book postures, hinge-aware layouts, and fold/unfold continuity
    - Chromebooks and desktop windowing: free resizing, multi-window
    - keyboard, mouse, trackpad and stylus: hover, focus, right-click context menus, shortcuts
    - no orientation or resizability locks
    - adaptive navigation that moves from bar to rail to sidebar
    - canonical layouts: list-detail, supporting pane, feed grid

    The template must *force* generated projects to stay adaptive: build-time code rules, multi-size previews and screenshot tests, and docs that require it. The same principle applies in the Compose Multiplatform template (iPad and desktop size classes) and the SwiftUI template (size classes and NavigationSplitView).
24. **Predictive back like Google's apps (Gmail).** The user wants this one. During the back gesture the current screen shrinks into a rounded card that follows the finger, the previous screen shows behind it, the card flies out on commit and springs back on cancel. Back-to-home matches the system animation. Recordings from the user's phone are in `research/predictive-back/gmail/`; the exact constants and implementation design are in `research/predictive-back/SPEC.md`. Owner: WP04. It must hold the same perf gates and work with BackGate (rapid back) and the tab bar.
25. **Document every smoothness finding where users and AI agents will follow it.** In each generated project (Android, Compose Multiplatform, iOS) and in the generator repo, `AGENTS.md`, `CLAUDE.md`, the other agents' entry files and the `docs/` guides must carry:
    - the smoothness rules and forbidden patterns, with do/don't code;
    - the root causes we found, each with its fix and the reason for it (tab rebuilds, the bar out of sync, alpha on full-screen layers, rapid back, cold first frames, glass costs, recomposition during animations, ripple overlays, stale gesture captures);
    - the measured before/after numbers on the reference phone;
    - the perf budgets and gates, and how to measure them (`perf.sh` on a release build, never debug);
    - the iOS motion catalogue with the exact springs, and the Gmail-style predictive back;
    - the adaptive layout rules and the recomposition budgets;
    - a pre-merge checklist an agent runs before calling UI work done.

    `AGENTS.md` keeps the short, enforceable version and links to the full guides. Owner: WP12, which reads every handoff note, every research plan, the QA defect lists and the perf data under `perf/`. Every package records its perf numbers and its "rules the docs must state" in its handoff for this purpose.

## Added by the user on Oct 9, 11:50
26. **Google's official Android agent skills** (github.com/android/skills, Apache-2.0) are installed on this machine as the Claude Code plugin `android-skills@android-skills`. Files are under `~/.claude/plugins/marketplaces/android-skills/`.
    - Agents use them. Navigation 3, navigation-event, adaptive, edge-to-edge, theming/styles, AGP 9 upgrade, R8 analyzer, testing setup, profiler and intent/permission security are the relevant ones.
    - The generator ships them too. Every generated project (Android and Compose Multiplatform) vendors the relevant skills with their licence under its agent directories (`.claude/skills/`, plus whatever the other agents' entry files need), and its `AGENTS.md`/`CLAUDE.md` tell agents when to use each. The generator repo's own `AGENTS.md` does the same.
27. **Google/AOSP-quality code and a deeper, more organised structure.**
    - The bar is code that would pass review in androidx/AOSP: the Compose API guidelines (androidx `compose-api-guidelines.md` and `compose-component-api-guidelines.md`), the Android Kotlin style guide, the official architecture guidance, and Now in Android-level organisation.
    - Deeper, well-organised package and folder structure inside every module.
    - Zero AI-slop comments.
    - Study AOSP and androidx sources for how they implement the equivalent components (pull to refresh, predictive back, transitions), and match that quality.
    - Reviewers judge against this bar.
28. **Tab switches get polished, optimised animation, not just an instant swap.**
    - A tasteful content transition between kept tabs (no rebuild, no double exposure, no offscreen alpha on full screens), plus selection motion on the bar: the indicator morphs or stretches toward its target, and the icon bounces on select.
    - It must hit the smoothness gates on the Redmi.
    - Record it.
29. **Pull to refresh needs a much better design, and its black shadow is a bug.** Recorded on the phone in `qa/ptr/`.
    - Today's version: a tiny 38dp disc over the content's text; the content does not move; the elevation shadow shows through the fading surface as a black ring, worst in dark mode; the offset is read in composition, so it recomposes every frame.
    - Target, iOS level: the content follows the finger with rubber-band resistance; the spinner is revealed progressively as you pull (UIRefreshControl spokes); a haptic tick at the threshold; on release the content rests at the spinner height while it refreshes, then springs back carrying its velocity; no surface disc, no shadow; correct in light and dark; zero recomposition per frame.
    - Owner: WP03. Record before and after.
30. **More visual defects seen on the phone:**
    - The DEV badge overlaps the large "Samples" title (devtools, WP06).
    - The selected tab's filled pill nearly fills the bar's height and crowds it (WP04).
31. **Material 3 policy (decided Oct 9, after the user asked).** Keep the custom foundation-only design system and do not depend on any Material *component* library (`androidx.compose.material`, `androidx.compose.material3:material3`, `material3-adaptive-navigation-suite`). The reasons: iOS-level look, no ripple, glass, Compose Multiplatform portability, and one token system.
    - **Allowed:** `androidx.compose.material3.adaptive:adaptive`, `adaptive-layout`, `adaptive-navigation` and `adaptive-navigation3`. Their Android POMs (stable 1.3.0) depend only on foundation, ui, window and navigation3, not on material3. Use them for window size classes and postures, and for the Navigation 3 `ListDetailSceneStrategy`/`SupportingPaneSceneStrategy`, as the adaptive skill prescribes. The build rules forbid `material3` component imports, not the `material3.adaptive` package.
    - **Optional later (wave 3, off by default):** a `materialbridge` feature, one module that maps our tokens into a `MaterialTheme` with ripple off, so a team can drop in a complex Material component without it looking foreign. Code rules confine Material imports to that module.
32. **Back swipe during a push (the user's screenshot, Oct 9 13:09, `qa/user/back-glitch-1309.png`).** Starting a back swipe while the push animation is still running makes the old shell show both screens half-transparent, shifted the wrong way, with a seam. Reproduced 3/3 on the old build (`qa/user/midback/bdp_compare.png`). The new WP04 build is clean (3/3) but simply holds the gesture until the push settles.
    - **Target:** a back swipe at any moment, mid-push included, runs the Gmail-style predictive back (REQUIREMENTS 24) from the screen's current position. Use Navigation 3 1.2.0's mid-flight predictive back where it is sound; otherwise retarget the push's spring into the predictive transition with velocity preserved.
    - Never translucent, never double-exposed, every entry opaque. Swipe edge respected: left-edge and right-edge swipes move the card toward the correct side.
    - Owner: WP04. Add a `back_during_push` scenario to `perf.sh` (tap, then the swipe 60–120 ms later; screenshot mid-gesture and assert no double exposure) and run it on the phone.

## Added by the user on Oct 12, 13:25

33. **A highly customisable, modern bottom sheet component in the generator.** It is the one sheet system of every generated app, so it must be the best part of the design system, not an afterthought.
    - **Specification:** `research/SHEET-SPEC.md`, written by a design workflow (three competing designs, judges, adversarial critique). Until it exists, the intent is:
      - iOS 26 feel in our own design language: detents (fixed height, fraction, fit-content, custom) chosen by velocity projection; rubber band past the top; the card stack behind a modal sheet; a grabber; haptics at detents; a floating inset sheet at partial detents that becomes edge-attached when expanded.
      - Modal and non-modal sheets (background interaction up through a detent, like Maps).
      - Header, footer and sticky-action slots; Solid, Frosted and Glass styles, with Glass through WP05's tiers.
      - Interactive dismiss with a confirm hook; nested sheets; Navigation 3 sheet destinations through metadata.
      - Gmail-style predictive back, IME-correct.
      - Adaptive: a centered form sheet or side sheet on larger windows, plus foldables and ChromeOS.
      - TalkBack actions.
      - Zero recompositions per drag frame; offsets in placement and scrim in draw, never full-screen alpha.
      - Parity: Compose Multiplatform, and a SwiftUI `appSheet` modifier over `presentationDetents`.
    - **Owners:** WP03 (the component, its state and tokens, the catalog demos), WP04 (the presenter, the Navigation 3 sheet scene, predictive back and BackGate, the card stack), WP05 (the Glass style), WP07-B (Multiplatform parity), WP08 (SwiftUI parity), QA2 (phone verification and recordings), WP12 (docs). Each package delivers its "Work split" section of the spec once the spec exists.
34. **The shimmer has issues and content gets cut (the user, Oct 12, 13:35, watching the phone).**
    - **Shimmer:** one coherent band must cross every placeholder in the same coordinate space, never a separate sweep per element sized to its width. It needs tokens for band width, angle, period and highlight per theme. Zero recomposition per frame. Skeletons shaped like the content they stand for, so nothing jumps or clips when it arrives.
    - **Content cut:** nothing is clipped by chrome (the bar, the large title, the status bar), by parent padding (insets go in `contentPadding`), by fixed heights or by clip shapes, in light, in dark and at font scale 1.5.
    - **Investigation, design and fix:** `qa/shimmer/` (`EVIDENCE.md`, `AUDIT.md`, `REPORT.md`, `VERIFY.md`). The shimmer fix is built on the side branch `wp/WP03-skel`, which WP03 merges. Clipping fixes are routed to their owners through handoff notes. Reviewers check both.
35. **Bring back the liquid lens distortion when navigating (the user, Oct 12, 14:20, on PeriFlow's after-early build).** Tapping a tab no longer refracts or distorts anything, and the user wants that effect back.
    - **Cause:** the audit's G1 fix (PeriFlow `GlassBottomBar`/`LiquidLens`, and the generator's `foundation/glass/LiquidLens.kt`: "A tap moves the lens with select and presses nothing") limited swell, refraction and sheen to a held (120 ms) or dragged lens. The phone was on the `Live` tier (logcat `perf.glass: tier=Live`), so no downgrade was involved. G1's real cost was the whole bar re-rendering for about 0.5 s with inner shadow and dispersion, not the lens itself. Removing the visible effect breaks REQUIREMENTS 21 ("smoothness must not come at the expense of animation"; glass morphing is in the list).
    - **Target, iOS 26 tab bar:** on a tap, the lens travels to the new tab as a liquid droplet. It lifts and swells slightly, refracts and magnifies what is under it while it moves, stretches along the direction of travel, and settles with the selection spring, all within the selection animation (about 0.3–0.4 s).
    - **Optimal implementation:**
      - Only the lens samples, within its own bounds, from the scene recorded once. The bar body is never re-rendered: no `layerBlock` reading pressure on the bar.
      - No dispersion or inner shadow on the tap path; the pressure pulse is driven in draw.
      - The same path serves taps and programmatic selection (back stack, deep link).
      - `LiveNoLens` and lower tiers keep today's lens-free tap.
      - A drag keeps the full press behaviour.
    - **Gates:** with `glass.quality=Live`, tab-tap janky % and GPU p90 stay within the `glass_live` gate of master plan §2. Record before and after frame strips that show the distortion on a tap.
    - **Owners:**
      - WP05: the generator's `LiquidLens`/`LiquidTabs`, plus the CP2 recordings, which include the tap morph.
      - WP11: PeriFlow's `GlassBottomBar`/`GlassRail`/`LiquidLens`.
      - WP07-B: the Multiplatform port.
      - QA2: verify it on the phone.
36. **Android first (the user, Oct 14, 12:10).** Make the generator live as soon as the Android work is 100% done, and only then. Compose Multiplatform and Swift continue afterwards and ship later.
    - **"100% done" for Android:**
      - every requirement here that applies to the Android template, the generator, the website and the API is met;
      - the smoothness goals hold (REQUIREMENTS 19 and 20 on the Redmi, release builds), as do security (WP06 plus the intent and permission security audits), adaptive UI, accessibility, the bottom sheet (33), the shimmer and clipping fixes (34) and the lens (35);
      - the docs are done (25);
      - the full Android matrix is green.
    - **Order:** WP13 verifies the Android release; an adversarial release gate writes `RELEASE-GATE-android.md`; **the user approves the deploy** (merge to main, push, Vercel). No agent pushes or merges to main.
    - **On the live site:** until phase B ships, the platform picker offers Android only, and Compose Multiplatform and Swift show "coming soon" (WP10).
    - **Phase B:** WP07-B and WP07-C, WP08's merge, WP12-B (multiplatform and iOS docs) and WP13-B (their verification), then a second gate and deploy. PeriFlow (WP11) is not part of either deploy.
37. **The full liquid glass effect, at the library playground's level (the user, Oct 15).** Restore every Kyant liquid-glass effect in the iOS-style components, the bottom bar first, with real similarity to the library's playground/catalog bottom bar (improved where possible): lens distortion and refraction, magnification, dispersion at the rim, specular highlight and rim light, the press swell, drag-to-select with velocity, and the travelling tap morph. Then glass top-bar buttons, toggle, slider, segmented control, buttons, sheet, alert and toast, plus a Glass playground section in the catalog.
    - **The rule:** full effects at the Live tier, made cheap rather than removed (REQUIREMENTS 35). Sampling within the lens or control bounds only, from the scene recorded once; no whole-bar re-render; dispersion and inner shadow only in the lens region; draw-phase animation. Lower tiers degrade gracefully. Behind `<opt:liquidglass>`.
    - **Owner:** a Fable deep-work agent on the side branch `wp/WP05-glass` (`handoff/WP05-glass.md`), with an Opus review and phone verification. WP05 merges it; WP11 ports the bar to PeriFlow in its second half.
38. **Google's Android skills v1.0.15 (the user, Oct 15).** Incorporate the new and updated skills (new: jetpack-compose-performance, glance-canonical-layouts, wear-widgets, android-for-cars-parked-apps, litert-runtime; updated: navigation-3, navigation-event, adaptive, android-profiler, testing-setup, camerax and others). Every project the generator produces vendors the skills its features use (feature-aware), and its `AGENTS.md`/`CLAUDE.md` say when to use each. The plugin on this machine is updated to 1.0.15; the digest is `research/ANDROID-SKILLS-DIGEST.md`; vendoring is on `wp/WP02` (workflow `android-skills-v1-0-15`). All agents apply the updated rules in their areas: jetpack-compose-performance for the smoothness work, navigation-3/navigation-event for WP04, adaptive for WP03/WP04.
    - **Oct 16 (the user): use the jetpack-compose-performance skill, not only vendor it.**
      - Every agent runs its audit on Compose UI changes.
      - Reviewers treat a violation in changed code as BLOCKING.
      - A template-wide audit writes `research/COMPOSE-PERF-AUDIT.md` and routes its findings to WP03, WP04, WP05 and WP02.
      - Every generated Android project vendors it, and its `AGENTS.md` tells agents to run the audit before a UI change is done.

39. **Cronet / HTTP/3 transport as an opt-in generator feature (the user, Oct 16).** Asked about Google's Cronet (Chromium's network stack, used by Google's apps), the user approved adding it as a requirement. The design agreed in chat:
    - **Opt-in, off by default:** a generator feature such as `network-cronet`. Most generated apps call one REST backend without HTTP/3, so the gain does not justify making it the default.
    - **Transport:** the template's networking is Ktor on the OkHttp engine, with Coil on OkHttp. When Cronet is available, install it into the shared OkHttpClient through Google's `cronet-okhttp` bridge (`CronetInterceptor` / call factory), so Ktor and Coil both get HTTP/2, HTTP/3 (QUIC), connection migration and Brotli without call-site changes. Prefer the platform `android.net.http.HttpEngine` on API 34+, then Play Services Cronet (`play-services-cronet`); fall back automatically to plain OkHttp when neither is available (no Play Services, e.g. Huawei and custom ROMs). Do not bundle `cronet-embedded` by default (several MB per ABI); at most as a documented option.
    - **Keep what the bridge bypasses working:** WebSockets stay on plain OkHttp (the bridge does not support them). Certificate pins are mirrored into Cronet's engine config, so security parity holds; the security audit checks this. The docs say which OkHttp network interceptors, cache and EventListener metrics do not apply on the Cronet path, and the template's own interceptors (auth, logging, headers) are application-level or moved so they still apply.
    - **Proof before recommending it:** measure with the template's perf tooling on release builds (cold and warm request latency, image loading, and a lossy-network case) against plain OkHttp, against an HTTP/3-capable endpoint. Add generator tests (the feature on and off: dependencies, wiring, fallback) and a unit test for the engine-selection fallback. Docs: when to turn it on, and its caveats.
    - **Part of the Android release (the user, Oct 17: "cronet one also include in Android release, I care about full android release now").** The release gate does not say GO without it.
    - **Brotli for every generated app, on by default (the user, Oct 17: "Yes, add Brotli support to the default template as part of NET").** Independent of the Cronet opt-in: the default OkHttp path accepts and decodes Brotli through `okhttp-brotli`'s `BrotliInterceptor` on the shared OkHttpClient, so Ktor and Coil both get it (`Accept-Encoding: br, gzip`; no second decoder such as Ktor's `ContentEncoding` on top). On the Cronet path, Cronet's own Brotli is enabled. Why: the user's Vercel deployment serves Brotli (checked Oct 17: `Content-Encoding: br` from the `bom1` edge) but not HTTP/3, so Brotli is the gain every app gets today. Proof: response size and latency against a Brotli-serving endpoint before and after on release builds, the APK size cost of the decoder, and generator tests for the dependency and wiring.
    - **Groundwork (Oct 18):** `research/NET-CRONET-BROTLI-NOTES.md` (versions on wp/WP06, the provider choice through
      Cronet's `HttpEngineNativeProvider` instead of an adapter, what the bridge bypasses, Brotli, measuring endpoints, risks
      such as the bridge's 500 ms idle wake-up). Claims are marked verified, inferred or unverified; NET checks the
      unverified ones before relying on them.
    - **Owners:** package NET in `scripts/to-release.js` (the network module, pinning parity, the generator feature spec and markers, tests and phone measurements; it starts once WP06 and WP02-M2 have merged, building on both), WP12 (docs), WP13 (verification).

40. **Live performance stats in dev and debug builds, plus a performance log and an optimisation score (the user, Oct 18: "we show a dev tag in dev env and debug builds can we show fps and other smoothness and performance stats there too. so people developing using my generator know how their app is behaving ... detailed logs of all performance and optimisation score u should give too").** In the Android release.
    - **Where:** next to the existing DEV badge (`:core:devtools`, WP06), in dev-flavour builds and every debug build; never compiled into a prod release (debug and dev source sets or `debugImplementation`-style wiring; the release APK of the prod flavour has no trace of it, checked by a test and an APK inspection). A generator feature, on by default, that a developer can turn off.
    - **The live overlay:** a compact pill beside the DEV badge with the current FPS at the display's real refresh rate (60/90/120 Hz) and the janky-frame percentage; tapping it opens a "Performance" tab in the existing dev panel with a frame-time graph against the frame budget, p50/p90/p99 frame time, slow and frozen frame counts, janky % per screen (route), the refresh rate, main-thread long tasks, memory (Java heap, native, PSS), GC count, cold and warm start (TTID/TTFD), and network timings from the existing devtools log. Built on JankStats/FrameMetrics (the template's `FrameJankRecorder` already uses JankStats), with the current route as JankStats state.
    - **It must not distort what it measures:** collecting runs off the main thread, the overlay updates a few times a second through draw-phase reads (no recomposition per frame), and hidden means no frame listener at all. Measured on the Redmi: overlay shown vs hidden adds at most 0.3 percentage points of jank and under 0.5 ms per frame on release-like builds. The perf gates and the final numbers are taken with it hidden; in devRelease and the benchmark build type it is off by default.
    - **Honest about debug builds:** debug builds are several times jankier than release builds (no R8, no baseline profile, interpreted code). The overlay says so on debug builds and points to `devRelease` for real numbers, instead of showing debug numbers as truth.
    - **Detailed log and score:** structured logcat output (one tag, periodic per-screen summaries) and an "Export report" action in the panel (also reachable by a debug-only adb command, not exported in release, per the android-intent-security skill) that writes a JSON and a Markdown report: every metric above per screen, the device and build facts, and an **optimisation score from 0 to 100** with a grade per metric against the template's own targets (REQUIREMENTS 19, `gates.toml`) and build checks (R8 on, baseline profile installed via ProfileInstaller, not debuggable, StrictMode violations), each lost point explained with the fix and a link to the docs (REQUIREMENTS 25).
    - **The overhaul's own report too:** `report/FINAL.md` (WP13) carries the detailed per-metric logs for each preset and the same optimisation score, old template vs new.
    - **Nothing in prod, not even idle cost (the user, Oct 18 14:35: "make sure these things dont display in prod and dont even consume any resume in prod").** This covers the whole dev tooling (the DEV badge, the dev panel, the network log, the performance overlay, its recorder, log and export), not only the overlay. A prod release APK contains none of its classes, resources, strings, manifest entries, receivers, providers, App Startup initializers or permissions (the modules are wired only into dev-flavour and debug variants, e.g. `devImplementation` / `debugImplementation` with a no-op or absent API in prod, never a runtime `if (BuildConfig.DEBUG)` around shipped code), and at runtime it starts no thread, timer, frame listener, logging or file write. Production telemetry that already exists (anonymous jank analytics in `:core:analytics`) stays as it is and gains nothing from this package. Proof, checked by the reviewer and by WP13: a build test that the prod release dependency graph has no devtools or HUD module; an inspection of the prod release APK (`apkanalyzer` or `dexdump` for classes and the manifest, `aapt2` for resources and strings) with zero matches; and on the phone, the prod release build shows no badge or pill and runs no extra thread (`adb shell ps -T`) compared with the same build before this package. Today (`wp/WP06`, Oct 18) `app/build.gradle.kts` wires `:core:devtools` with `implementation(...)` into every variant and gates it at run time, so the variant wiring for all dev tooling is HUD's job (it builds on `wp/WP06`); WP06 merges as it is and does not change that wiring, so the two branches do not collide.
    - **First, and visible (the user, Oct 18 14:20: "start with tha fps and performance rwquirement first i wanna see it first include graphs and all but make sure logging such things dont drop the actual performance of apps draw and render logs graphically when user rwquets").** Built ahead of the merge order by `scripts/hud-first.js` on `wp/HUD` (branched from `wp/WP06`), ending with a phone demo for the user (`perf/HUD/SHOW.md`, screenshots, a recording). Graphs: frame time against the budget with 60/90/120 Hz lines, FPS over time, a frame-time histogram, janky % per screen, memory over time. They are drawn only while the Performance tab is open; collection writes fixed-size primitive ring buffers with no per-frame allocation or logging, so recording costs the app nothing measurable.
    - **Owners:** package HUD in `scripts/to-release.js` (starts once WP06 and WP04 have merged: it builds on WP06's devtools and analytics and WP04's perf tooling and gates), WP12 (docs: what each number means, how to read the score, debug vs release), WP13 (verification, overhead measurement, FINAL.md score). Phase B: the same overlay for Compose Multiplatform desktop and web.

41. **iOS-like scrolling on every scrollable surface, by default (the user, Oct 18: "also need smooth scrolling like ios").** Makes REQUIREMENTS 21's "rubber-band overscroll, and scroll deceleration that matches iOS" and REQUIREMENTS 19's scroll targets concrete:
    - **Feel:** iOS deceleration (`UIScrollView.DecelerationRate.normal`, about 0.998 per ms; WP03's `exponentialDecay(0.4767)` with the 30 px/s rest velocity that fixed the "tap ignored after a gentle scroll" bug) is the default fling for every list, grid, pager and scrollable column in the template and the catalog, not opt-in; the rubber band (`dy = dx · 0.55 · (1 − y/d)²`, `research/ios-motion.md` §3) is on by default at both edges, a fling into an edge carries its velocity into the rubber band and springs back without a jump; nested scrolling (lists in sheets, collapsing large titles, pull to refresh) hands velocity over without a stall; tapping the status bar area or re-tapping the active tab scrolls to the top with iOS's curve.
    - **Smoothness:** scroll janky frames ≤ 0.5% at 120 Hz on the Redmi on release builds (aim ~0%), GPU p90 ≤ 7 ms, no hitch on the first fling after a cold start or `pm clear` (the deep-smoothness work, `wp/WP04-perf`), and no recomposition per scroll frame.
    - **Proof:** phone recordings and numbers (fling distance and duration for a set of velocities compared with the iOS curve from `research/ios-motion.md`, edge overscroll traces, the scroll gates) on release builds, plus unit tests for the physics.
    - **Owners:** WP03 (the physics defaults and every component), WP04-perf (scroll jank and the first-fling hitch), QA2 (feel check on the phone against the iOS reference), WP12 (docs: how to opt a surface out). Phase B: the same physics in the Compose Multiplatform template.
42. **Perf release gates and the back gesture (the user, Oct 19 06:16, answering two questions with pros and cons).**
    - **Back gesture: adopt the texture.** Apply `out/WP04-perf/backcard-texture.patch` (`BackCardState.applyCard`:
      `CompositingStrategy.Offscreen` for the whole time the back card is active, in place of `ModulateAlpha` during
      the fade only) on `wp/WP04`. Measured on the Redmi (WP04-perf §2h, phase 17b): RenderThread hitches 38 -> 2,
      janky after the first frame 0.8-0.93% -> 0.4-0.53%, total 2.4% -> 2.0-2.1%. Accepted costs: the card's first
      frame about one vsync later, about 10 MB of GPU memory while the card is up, text from a texture scaled 0.9-1.0.
      Rule A11.3.1 (no offscreen layer on a whole screen) gets a written exception limited to the back card, with the
      reason (rendered once per gesture, not every frame) in the rule's text, the code comment and the docs.
    - **Gates: gate what users feel.** In the Android release gate and in `tools/perf/gates.toml`, the release limits
      for tab switch, push/pop and back become the hitch ratio (`*.hitch_ms_per_s`) and the jank after a motion's first
      frame (`*.janky_after_first_pct`), with `*.late_first_frames` reported. Total `*.janky_pct`, first-frame, p99 and
      app PSS rows become non-regression ratchets at the values measured at the merge (they may only improve). The
      evidence: an empty-content build misses the first-frame deadline on 7 of 7 tab switches from idle on this phone,
      and graphics buffers alone take 53-56 MB of the 48 MB PSS row. Scroll keeps its own limits (REQUIREMENTS 41).
    - **The goal does not change (the user, Oct 19 06:20: "Make sure the app is light and smooth that's it. Goal is still
      the same most optimised visual feel").** A ratchet is a floor, not a finish line: keep cutting memory (app PSS,
      native heap: glyph caches and GPU programs), APK size, startup and every janky frame wherever a fix exists, and
      prefer the lighter of two equally smooth designs. The gates only stop the release from being blocked by limits
      of the phone itself.
    - **Owners:** WP04 (applies the patch, the A11.3 exception, `gates.toml`, re-runs acceptance), WP04-perf (supplies
      the exact limits from its §3), QA2 and the final matrix (use these gates), WP12 (docs).
43. **The catalog app shows everything the overhaul built, at the Google bar (the user, Oct 19 15:40: "update the quality of catalog app with all the changes we have done thats in the plan").**
    - **Now: a preview build** (`scripts/catalog-preview.js`): every package done so far (WP03 with the scroll fix and iOS scrolling,
      WP04 with the back card T + C1, WP06, HUD) combined on a throwaway local branch, the catalog release APK built,
      installed on the Redmi and sent to the user. Never pushed, never merged; rough edges are reported, not hidden.
    - **Then: the content, properly** (package CAT in `scripts/to-release.js`, after WP03, WP04, WP05, WP06 and HUD merge): a page
      for every piece a developer should see and feel (design system, iOS scrolling, sheets and toasts, carousel and pickers,
      glass tiers, the navigation shell and the back card, adaptive layouts, the performance overlay in dev builds, and
      whatever else merged). Real interactive demos, AppTheme only, no Material, light and dark, RTL, font scale, TalkBack.
      Proven on the phone with a release build (screenshots of every page, a tour recording, perf.sh rows within the
      release gates). QA2 checks it; WP12 documents it.
44. **Staging first: every package combined, tested and shown before new work (the user, Oct 19 15:50).** "FIRST TARGET IN NEXT SESSION IS TO FINISH UP WORK AND MERGE ALL CODE TO A SINGLE BRANCH "STAGING" U NEED TO CREATED AND THEN CREATE WP AND OTHER BRANCHES FOR THAT STAGING TO CONTINUE WORK, I WANNA SEE ALL CHANGES COMBINED AND TESTED AND THEN ONLY NEXT WORK CONTINUES".
    - Their choices, asked with pros and cons: every package with work goes in (PeriFlow never); one combined Google/AOSP-level
      review of the merged code replaces the per-package reviews still open (their open points move to the new branches);
      staging is pushed as a backup; old branches stay untouched as history and new work branches `staging-wp/<package>` are
      cut from staging, which becomes the integration branch.
    - Done by `scripts/consolidate-staging.js`: clean stop points (WIP commits), the merge plan, the merges with compile
      checks and notes, the desktop matrix and the combined app on the phone (perf gates, scroll direction, back gesture,
      overlay, every catalog page), a review by area with a fix loop, the new branches, and `staging/STAGING-REPORT.md`.
    - No package work continues until the user has seen the report and says go; then the release workflow is re-based on
      staging.

45. **Learn from District by Zomato's motion, with the user's approval for every change (the user, Oct 19 17:00).** "the animations fluidity of one app that i use is so great and amazing and everything polished in that app ... its called District by Zomato ... i like its bottom sheet and other components as well ... what we can incoporate from there to here. Every change suggested from that app in my project should go through my approval and i wanna know what they are doing diffrently. u can even install that app on my device after ur resaerch".
    - `scripts/research-district.js`: public sources (Sonnet lookups, an Opus verifier), a gap analysis against the template and
      proposals (`research/district/RESEARCH.md`), a phone-study procedure (`research/district/PHONE-STUDY.md`), an Opus review.
      No code changes.
    - Every proposal is asked in chat (pros, cons, catches, recommended first); only approved ones become work, on
      `staging-wp/*` after staging.
    - The phone study runs after the bug fix and staging free the phone, once the user has installed District from the Play
      Store and signed in themselves (I never sign in or create accounts). It only observes: no decompiling, no copying of the
      app's code or assets, no taps that buy, book, pay or change the account.

    - **The user, Oct 19 20:27:** "next time we start new session u will find district signed in and installed except making orders and doing any payment or booking u can use it freely the app to understand the animations and all". The phone study may use District freely (browse, open sheets, scroll, navigate, record, measure) once it is installed and signed in; never order, pay, book or change the account. Run it after the bug fix and staging have finished with the phone (Sonnet operator under research/district/PHONE-STUDY.md, Opus analysis of the curves).

46. **Every requirement fully fulfilled, the most optimised, best and smoothest way (the user, Oct 19 19:56).** "make sure requirements are fully fulfilled in their most optimises best and smoothest way".
    - Staging checks it (`scripts/consolidate-staging.js`, phase Requirements): three Opus auditors judge every requirement and
      sub-point on the combined code as met (with proof: a test, a phone capture or a perf number), partly, not-met or deferred
      (only by a user decision), plus whether it is done the smoothest way on the Redmi and, if not, the better way and its cost; a
      separate Opus reviewer rejects any "met" without proof and any "best" over a red perf row. Output
      `staging/REQUIREMENTS-MATRIX.md` and `staging/REQUIREMENTS-GAPS.md` (gaps by staging-wp branch).
    - The Android release gate requires every Android requirement met with proof, done the smoothest way the evidence supports,
      or deferred by the user; the release workflow, re-based on staging after the user's go, re-runs the matrix as its last step.
    - Every package run after staging starts from its gaps in REQUIREMENTS-GAPS.md and closes them with proof.

47. **Decisions of Oct 19 22:08 (the user, asked in chat with pros, cons and catches).** The overhaul-workspace history is scrubbed of the raw District lookups that name people (done 22:12); an agent picks CP2 (the glass look) from the recordings and the user can veto; the repository gets an MIT LICENSE, 'Copyright (c) 2026 Sarthak Tripathi'; generated keystore passwords stay printable ASCII. Still to ask: the Utility bar (Docked to Floating, e80fe43), the Kyant playground install, the Gradle 9.8.1, protobuf and npm audit approvals, whether the old emulator's fold and posture results suffice, and the smaller items in audit/2026-10-19-coverage/COVERAGE-AUDIT.md; the Vercel settings (ENABLE_TARGETS, ADMIN_TOKEN, DATABASE_URL, TRUSTED_PROXY) are the user's to set before the release.
    - Second batch (22:16): keep the Utility style's Floating bar; the user installs the Kyant playground for the side-by-side strips; approved: the npm security upgrades, the Gradle 9.8.1 download, protobuf in a project venv; the old emulator's fold and posture results suffice for the Android release.

48. **Learn from Telegram for Android and exteraGram, with the user's approval for every change (the user, Oct 19 23:08).** "There is this app called exteragram ... known for its good transitioning animations fluidity and all. Launch a agent for understanding that app as well and what good bits we can take. It's hella smooth. Even with enormous data ... we are always gonna be 100% compose but we can definitely extract the good bits from Java xml ... Even the original telegram app is hella smooth and superb animations, it's those things that I like."
    - `scripts/research-telegram.js`: Sonnet inventories of the source (downloaded with the user's OK, Java tree only, 156 MB: Telegram f32ffee of Oct 13 2026, exteraGram 6f78031 of Jun 3 2023, its public source being old) and the web; Opus verifier, comparison and proposals (`research/telegram/RESEARCH.md`), a phone-study procedure, an Opus review.
    - GPL-2.0 source, MIT template: ideas, techniques and numbers only, re-built in Compose; never copied code. The download stays out of every backup and goes to the Recycle Bin after the research.
    - Every proposal is asked in chat one by one; approved ones land on staging-wp branches after staging. A phone study needs the user's own install and sign-in and records no one's messages or names.
    - **The user, Oct 19 23:14:** "U can forget exteragram, it's mostly famous for smooth blurs and all, telegram is the goat of animations". The research covers Telegram only; exteraGram's source went to the Recycle Bin.
    - **The user, Oct 19 23:16:** "Over the actual animations look for optimisation." The research leads with how Telegram makes motion and scrolling cheap (snapshots and layers during transitions, no re-layout or re-binding, off-main-thread work, pre-built state, device tiers), with Compose equivalents and their per-frame savings on the Redmi; look-and-feel parameters come after.

49. **The performance overlay shows a recomposition score, flags what is not optimised, and adds other cheap signals (the user, Oct 20 00:33).** "you know we have showing the matrix like the FPS in jank rate in the app, can we also show recomposition score and if it's not optimised and ki other performance stuff".
    - Designed now by `scripts/hud-metrics-design.js` (Sonnet API lookup, Opus design, Opus review; `research/hud-metrics/DESIGN.md`); built on `staging-wp/HUD` after staging and the user's go (REQUIREMENTS 44).
    - The overlay must cost nothing while hidden, never recompose app content or allocate per frame, and be absent from release builds and every build the perf harness measures, proven by an A/B run on the Redmi (the Oct 19 jank regression's first suspect is the overlay in the measured build).

50. **Everything learned about smoothness goes into the docs (the user, Oct 20 01:06).** "Whatever changes telegram research brings and I approve add them in the docs too regarding how to make smooth and keep the project running smooth..updated the docs too with all knowledge later."
    - Every approved proposal (Telegram, District, the overlay metrics, and any later one) ships with its documentation in the same change: the rule, why it keeps the app smooth, how to apply it in a screen, the anti-pattern, and the number it protects. A proposal is not done without its docs.
    - Where: the generated project's performance guide and AGENTS.md rules (so apps built on the template stay smooth), and the website's docs; owned by the package that builds the change, reviewed by the docs owner (WP12).
    - Later, a knowledge pass (WP12, after the staging-wp work and before the release gate) folds all of the overhaul's research into those docs: MOTION-PLAN, SHEET-SPEC, SMOOTHNESS-PLAN, the back gesture design, the District and Telegram findings, the overlay metrics, the perf gates and how to read them, and the lessons in decisions/LOG.md. The requirements matrix checks it.
