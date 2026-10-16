# Audit: template app shell and navigation (`template/app/**`, `template/core/navigation/**`)

Scope: `MainActivity`, `BaseAppApplication`, `ui/AppRoot.kt`, `ui/AppDestinations.kt`, `deeplink`, `di`,
`session`, `lock`, `push`, `playstore`, `widget`, and all of `core/navigation`. Compared against the
hand-improved apps PeriFlow (`periflow-android`) and VitFleet (`vitfleet-android`).

Path prefixes used below:

| Prefix | Absolute path |
|---|---|
| `T/` | `/home/crazyhusir/Documents/projects/tools/android-base/template/` |
| `PF/` | `/home/crazyhusir/Documents/projects/jorwen/periflow/periflow-android/` |
| `VF/` | `/home/crazyhusir/Documents/projects/tools/vitfleet/vitfleet-android/` |
| `NAV3/` | `/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/nav3src/` (Navigation 3 1.1.7, lifecycle 2.11.0 sources unzipped read-only from `~/.gradle/caches`) |

Library versions are identical in all three projects (`navigation3 = 1.1.7`, `androidxLifecycle = 2.11.0`,
`composeBom = 2026.09.00`, `T/gradle/libs.versions.toml:21-24`), so every library behaviour cited
below applies to the template exactly as it does to PeriFlow and VitFleet. Nothing was built and no Gradle was run;
numbers in tables come from closed-form spring and easing maths (scripts in
`/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/spring_seek.py` and `tabfade.py`) and from the
PeriFlow commit messages, which report on-device `gfxinfo` and Perfetto measurements.

---

## 0. Summary: why tab switches and push/pop feel "glitchy and pushy"

Ranked by how much each one shows on screen:

1. **Every tab switch rebuilds the incoming tab from nothing and cross-fades two full-screen layers.**
   `TabbedNavHost` wraps each tab's `NavDisplay` in `AnimatedContent(targetState = selectedIndex)`
   (`T/core/navigation/.../AppNavHost.kt:99-106`). AnimatedContent keeps only the target, plus the content that is
   leaving, so the outgoing tab's `NavDisplay` and all of its screen composition are disposed, and the incoming tab's
   are composed from scratch on the frame of the tap. The `tabSwitch` spec (`NavTransitions.kt:105-113`) then
   fades the old tab out in 135 ms and the new one in after a 90 ms delay, which lets the window show through
   by up to **30%** at t≈90 ms (a visible blink). It also scales the new tab from 0.985 and puts both tabs in
   alpha layers, which are rendered offscreen. PeriFlow measured this exact code: **12.4% janky frames, p99 117 ms** on tab
   switches, falling to **~4%, p99 61 ms** once tabs were kept composed and the switch made instant (commit `a8aedb0`).
2. **The bottom bar moves along its own axis on its own clock.** `showBar = alwaysShowBar || onRoot`
   (`AppShell.kt:175-176`) flips at the moment the stack changes. The bar then slides vertically by its full height
   on an underdamped sheet spring (`AppShell.kt:230-232`, `AppMotion.sheet()` damping 0.86, or 0.68 in Bouncy), while
   the screen slides horizontally. On a programmatic pop the bar starts rising while the screen is still
   leaving. After a predictive-back swipe the stack only changes *after* the gesture ends, so the bar rises
   after the screen has already gone: "the screen arriving twice", as PeriFlow's `dec4ff5` puts it.
3. **Push and pop fade a full-screen layer.** The outgoing screen fades to alpha 0.72 (`NavTransitions.kt:30-33`, `43-46`).
   That makes the whole screen render offscreen on every frame. Because the window behind it is light in the light theme,
   the screen turns *pale* rather than receding (VitFleet `NavTransitions.kt:27-29` says the same). The slide uses a spring
   whose settle takes **~466 ms** (Standard), and 95% of the travel is done by 238 ms. Half the transition is a sub-5% crawl,
   during which the lifecycle stays capped at STARTED and Back is unsafe (point 5).
4. **Predictive back scrubs a spring.** `predictivePopTransitionSpec` uses the same spring-based `pop()`
   (`AppNavHost.kt:126-129`). NavDisplay maps gesture progress linearly onto the transition's play time
   (`NAV3/ui/.../NavDisplay.kt:748-752`), so with the finger 30% across the screen is **77%** of the way out. With Bouncy
   (`navigationDamping = 0.86`, `AppMotion.kt:55`) it overshoots and shivers. PeriFlow's `eb2d1ee` describes and fixes this exact symptom.
5. **A back gesture during a push re-targets the running transition.** NavDisplay calls
   `transitionState.seekTo(progress, previousScene)` while `animateTo(scene)` is still running
   (`NavDisplay.kt:748-757`). The screen sliding in vanishes or slides the wrong way, then snaps. The template has
   no guard; PeriFlow added `BackHold.kt` (`84a9483`, `8943957`).
6. Secondary causes: the modal slide overshoots and opens a gap at the bottom (12 px Standard, **130 px** Bouncy).
   In rail mode, content slides over the rail. Any change of window width class disposes every tab. Signing in or out swaps the
   whole host with a hard cut. The bar's tab items flash a grey rectangle on press and recompose every
   frame while their selection animates.

The fixes, in order: keep tabs composed and switch instantly (PeriFlow `KeptTabs`, with improvements); stop the bar
moving separately from the screens; use slide-only push and pop on a shorter critically damped spring; use a linear predictive pop;
hold Back while the stack is moving; call the host from exactly one place (VitFleet). Each is
specified in §7.

---

## 1. How navigation works today: mechanics traced

### 1.1 Composition root

`MainActivity.onCreate` (`T/app/.../MainActivity.kt:116-184`):

- Holds the splash until `startup` (a `MutableStateFlow` **field of the Activity**, `:107`) receives the first
  `combine(settingsStore.settings, tokenStore.isAuthenticated)` value (`:123-129`).
- `setContent` returns early while `startup` is null (`:147-148`), then calls `AppRoot(startKey, tabs, ...)` with
  `Startup.entryPoint()` (`:239-247`). That function returns `(OnboardingKey, [])`, `(SignInKey, [])` or `(start, tabs)`.
- Calls `handleDeepLink(intent)` on **every** `onCreate` (`:182`), and calls it again from `onNewIntent` (`:219-223`).

`AppRoot` (`T/app/.../ui/AppRoot.kt:50-93`) branches on `tabs.isEmpty()`:

- Single stack: `rememberAppBackStack(startKey)` (a `rememberSaveable` keyed on `startKey`, holding JSON) feeds
  `AppNavigationHost`, plus a `signedOut` collector that does `ResetTo(signInKey)` (`:52-66`).
- Tabs: `rememberShellState(tabs)` feeds `AppShell`, plus a `signedOut` collector that resets every tab and then pushes
  `ResetTo(signInKey)` into the current tab (`:68-85`).

These are two different composition subtrees. Moving between them (onboarding to sign-in to tabs, or sign-out)
disposes one host and builds the other, with no transition.

### 1.2 Shell state

`ShellState` (`AppShell.kt:57-96`) holds one `SnapshotStateList<AppNavKey>` per tab plus `selectedIndex`. Every
`NavCommand` from the singleton `AppNavigator` channel is applied to `state.current`, the selected tab
(`AppShell.kt:171-173`). The saver writes `"index|[[...],[...]]"` and restores by index, checking only that the number
of tabs matches (`AppShell.kt:114-150`).

### 1.3 Tab host

```kotlin
// T/core/navigation/.../AppNavHost.kt:87-106
val tabEntries = state.stacks.mapIndexed { index, stack ->
    key(index) { rememberDecoratedNavEntries(stack, rememberEntryDecorators(), rememberEntryProvider(...)) }
}
AnimatedContent(targetState = state.selectedIndex, transitionSpec = NavTransitions.tabSwitch(motion)) { index ->
    StackDisplay(entries = tabEntries[index], onBack = onBack)    // a NavDisplay per tab
}
```

- The decorators (saveable-state holder plus ViewModel store) are hoisted above `AnimatedContent`, so ViewModels and
  `rememberSaveable` state survive a tab switch. Entries are only rebuilt when the stack contents change
  (`NAV3/runtime/.../DecoratedNavEntries.kt:125`, `remember(backStack.toList())`). A decorator's `onPop` runs only when an
  entry is both off the stack *and* out of composition (`:211`). That is correct.
- Everything inside `AnimatedContent` (the `NavDisplay`, its `SeekableTransitionState`, scene state, and the screen's
  `remember`, `LaunchedEffect`, layout and draw caches) is **disposed for the outgoing tab and recreated for the incoming
  one** on every switch. This is root cause 1.

### 1.4 The stack display: Navigation 3 internals that matter

`StackDisplay` (`AppNavHost.kt:109-131`) passes `sceneStrategies = listOf(rememberListDetailSceneStrategy(),
SinglePaneSceneStrategy())`, plus `push`, `pop`, and `pop` again for the predictive gesture. Inside NavDisplay 1.1.7:

| Behaviour | Source | Consequence for the template |
|---|---|---|
| One `SeekableTransitionState(scene)` drives an `AnimatedContent` keyed by `(scene class, scene.key)` | `NavDisplay.kt:623-630, 818-821` | Scenes with the same key, such as a list-detail scene that swaps its detail, do not animate at all |
| `isPop` is true only when the new stack is a strict prefix of the old; if the first entry changes, the move counts as a push | `NavDisplay.kt:653-660, 903-914` | `ResetTo(...)` (sign-in, sign-out, onboarding) plays a *push* |
| z-index is +1 for a push and -1 for a pop or predictive back | `NavDisplay.kt:667-677` | Pop ordering is already right. **Any `targetContentZIndex` or `sizeTransform` set inside a spec is discarded**, because NavDisplay rebuilds the `ContentTransform` (`:822-829`) |
| Spec resolution: the entry or scene metadata of the scene with the higher z-index first, then the NavDisplay default | `NavDisplay.kt:740-745, 797-812`; `Scene.kt:106` (scene metadata = last entry's metadata) | Push and pop use the incoming or outgoing entry's style correctly |
| Predictive back: `LaunchedEffect(previousScene, progress) { seekTo(progress, previousScene) }` | `NavDisplay.kt:748-752` | Seeking maps progress linearly onto the spec's *time*, so springs track non-linearly (root cause 4) |
| When a gesture completes or is cancelled, the remainder plays on a **linear** `tween(remainingDuration)` | `NavDisplay.kt:763-791` | The finish is always linear; this cannot be changed in 1.1.7 |
| A gesture that starts while `animateTo` is running re-targets the seekable state | `NavDisplay.kt:748-757` | Root cause 5 |
| Scene lifecycle is capped at STARTED until `currentState == targetState` | `NavDisplay.kt:835-841` | During the ~466 ms spring tail, the new screen is not RESUMED |
| Scenes are recomputed when `sceneStrategies.toList()` or the entries change | `NAV3/ui/.../SceneState.kt:104` | `SinglePaneSceneStrategy()` is a new instance on every recomposition and has no `equals` (`SinglePaneScene.kt`), so every `StackDisplay` recomposition recalculates every scene |

### 1.5 Bottom bar and insets

- In compact width the bar is an overlay in a `Box` above `TabbedNavHost` (`AppShell.kt:218-239`). Content is never
  re-laid out by the bar's visibility, so there is no content jump. The jank is the bar's own motion.
- Only a tab's **root** entry is wrapped in `Box(Modifier.fillMaxSize().background(...).padding(bottom = inset))`, where
  `inset = AppBottomBarDefaults.occupiedHeight()` (`AppNavHost.kt:93, 153-168`; `AppBottomBar.kt:80-87`). Pushed
  entries get nothing, and in single-stack mode no entry gets anything.
- In medium width and up, a `Row(rail, TabbedNavHost)` is used instead (`AppShell.kt:195-216`). It is a **different call site** for
  `TabbedNavHost`.

---

## 2. Root causes in detail

### RC1: Tab switch rebuilds the tab and blinks *(high, performance and ux)*

Evidence:

- `T/core/navigation/.../AppNavHost.kt:99-106`: `AnimatedContent` around `StackDisplay(entries = tabEntries[index])`.
- `NavTransitions.kt:105-113`: `fadeIn(tween(medium, delayMillis = instant)) + scaleIn(0.985)` together with
  `fadeOut(tween(instant + instant / 2))`.
- Opacity maths (`scratch/tabfade.py`), the share of the background visible through both tabs:

  | Style | Worst blink |
  |---|---|
  | Standard | 30% at t=90 ms |
  | Calm | 30% at t=110 ms |
  | Snappy | 30% at t=60 ms |
  | Bouncy | 30% at t=90 ms |

- `fadeIn`, `fadeOut` and `scaleIn` apply a `graphicsLayer` alpha to a full-screen subtree. With overlapping content,
  that is an offscreen buffer for **both** tabs on every frame of the switch, on top of the first composition of
  the incoming tab. If the outgoing tab was in the middle of a push, the push's own alpha layer is nested inside.
- PeriFlow `a8aedb0`: "a tab change composed a whole screen from nothing on the main thread … Tab switching went
  from 12.4% janky frames (99th percentile 117 ms) to about 4% (61 ms)."
- Side effect: for 135 ms both tabs' `NavDisplay`s are composed, and each registers a back handler. The most
  recently registered *enabled* handler wins (`PF/.../KeptTabs.kt:381-385` documents this ordering), so a Back pressed
  just after switching away from a deep tab pops the **hidden** tab.

Fix: keep opened tabs composed, make the switch instant (iOS behaviour), and make the bar's indicator carry the
motion. See §7.2 for an improved `KeptTabs`.

### RC2: The bar animates separately from the screens *(high, ux)*

Evidence:

- `AppShell.kt:175-176`: `val onRoot = state.current.size == 1; val showBar = alwaysShowBar || onRoot`.
- `AppShell.kt:227-233`: `enter = slideInVertically(motion.sheet()) { it } + fadeIn(...)`, and
  `exit = slideOutVertically(tween(motion.medium, easing = motion.exit)) { it } + fadeOut(...)`. `AppMotion.sheet()` is
  `spring(dampingRatio = sheetDamping, stiffness = 460f)` (`AppMotion.kt:145-146`), with `sheetDamping` 0.86 by default and
  0.68 in Bouncy (`:54, :121`), so the bar *bounces* into place.
- Timing: a programmatic pop changes the stack first and animates second, so the bar starts rising while the screen is
  still sliding out. A predictive pop changes the stack only when the gesture completes (`NavDisplay.kt:559-565`),
  so the bar rises after the screen has left.
- The fade on the bar also forces an alpha layer, which is expensive when the bar is glass (§6).
- PeriFlow `dec4ff5` acknowledges the symptom and reduces it rather than removing it: a quick tween with a rise of a third
  (`PF/core/navigation/.../AppShell.kt:290-295`). The bar is still tied to stack depth.

Fix (§7.3): by default the bar stays at every depth, as on iOS, where `hidesBottomBarWhenPushed` is opt-in. Pushed
screens get the same content inset. For destinations that must hide it (`FullScreenKey`, from VitFleet), the bar moves
**with the root screen** in the draw phase rather than on its own animation.

### RC3: Push and pop fade a full-screen layer and settle slowly *(medium, ux and performance)*

Evidence:

- `NavTransitions.kt:30-33`: `fadeOut(targetAlpha = 1f - motion.outgoingDim)` on the outgoing screen of a push.
  `:43-46` is the mirrored fade-in on pop.
- Window background: `#F7F8FA` light, `#0B0E14` dark (`T/app/src/main/res/values/colors.xml:4`,
  `values-night/colors.xml:4`). An alpha fade towards a light window *lightens* the receding screen.
- `VF/core/navigation/.../NavTransitions.kt:25-29`: "No alpha on either: fading a screen towards the window colour
  reads as it turning pale, not as it falling behind."
- `PF a8aedb0`: "Push and pop only slide: fading an opaque glass screen rendered all of it offscreen each frame for
  nothing visible."
- The slide spring is `spring(navigationDamping, navigationStiffness)` with the default 1 px `IntOffset` threshold.
  On a 1080 px slide (`scratch/spring_seek.py`):

  | Style | Settle to 1 px | 95% of travel at | 99% of travel at |
  |---|---|---|---|
  | Standard (k = 400) | 466 ms | 238 ms | 332 ms |
  | Calm (k = 300) | 538 ms | 274 ms | 384 ms |
  | Snappy (k = 700) | 352 ms | 180 ms | 251 ms |
  | PeriFlow Fluid (k = 340) | 505 ms | 258 ms | 361 ms |

  The fade is a 250 ms tween, so on the outgoing screen the alpha finishes long before the slide.

Fix: slide only. A stiffer critically damped spring (k ≈ 600–700) settles in ~350–380 ms, which shortens the lifecycle cap
and the Back-hold window. If depth is wanted, draw a cheap 8–12 dp edge shadow on the incoming screen (a gradient in
`drawBehind`) instead of an alpha fade.

### RC4: Predictive back scrubs a spring *(high, bug)*

Evidence:

- `AppNavHost.kt:126-129`: `predictivePopTransitionSpec = { _ -> NavTransitions.pop(motion)() }`. For push entries,
  `metadataFor` returns `emptyMap()` (`:180`), so the default applies. For modal entries it is
  `predictivePopTransitionSpec { _ -> exit() }` with the spring-based `modalDismiss` (`:185-187`).
- Seek maps progress p to play time p·T (`NavDisplay.kt:751`). Share of the slide covered at gesture progress p:

  | Style | p = 0.1 | p = 0.2 | p = 0.3 | p = 0.5 |
  |---|---|---|---|---|
  | Standard | 24% | 56% | **77%** | 95% |
  | Bouncy | 24% | 58% | 81% | 99% (then overshoots) |

- `PF eb2d1ee`: "a third of the swipe put the screen nine tenths of the way out and it shivered as the finger moved".
  The fix is in `PF/core/navigation/.../NavTransitions.kt:251-268` (`tween(medium, LinearEasing)`) and in VitFleet
  `NavTransitions.kt:59-82` (linear, plus a 0.92 scale on the leaving screen).

### RC5: A back gesture taken mid-push breaks the transition *(high, bug)*

The template has nothing equivalent to PeriFlow's `BackHold.kt`. NavDisplay re-targets a running `animateTo` when a
gesture starts (`NavDisplay.kt:748-757`). PeriFlow (`84a9483`) reports that the new screen "slid the wrong way, paused and
snapped". In `8943957` it adds that a cancelled swipe "slid it in again as if it had just been opened". With the
template's ~466 ms settle (RC3), that window is long. `PF/core/navigation/.../BackHold.kt` holds Back while any entry of the
stack reports that its enter or exit transition is running, then replays it as a plain pop. It has unit tests:
`PF/core/navigation/src/test/.../BackHoldTest.kt`.

### RC6: Overshoot artefacts *(medium, bug)*

- Modal: `slideInVertically(animationSpec = motion.sheet(), initialOffsetY = { it })` (`NavTransitions.kt:59-62`)
  slides a full-height screen on an underdamped spring. The overshoot is exp(-ζπ/√(1-ζ²)) of the travel: **0.5%
  (12 px on a 2400 px window) for Standard, 5.4% (≈130 px) for Bouncy, 1.1% for PeriFlow Fluid**. The overshoot lifts the
  modal's bottom edge and shows the screen underneath for a few frames.
- Bouncy navigation: `navigationDamping = 0.86f` (`AppMotion.kt:55`), although the property's own doc says "A long
  travel with any bounce in it reads as unstable" (`:123`). The overshoot is about 0.5% (~5 px on 1080 px). The incoming screen
  passes its rest position, which shows a sliver of the outgoing one, and with predictive back it shivers (RC4).

Fix: every full-screen translation uses damping 1. Bounce stays on presses, pills and lenses.

### RC7: Structural rebuilds the user sees as glitches *(medium)*

1. **The width-class branch disposes every tab.** `AppShell.kt:195-216` calls `TabbedNavHost` inside a `Row` when
   `width >= Medium`, and `:218-225` calls it in the `Box` otherwise. The call sites differ, so the compositions differ.
   When the width class changes without the Activity being recreated, the whole host is disposed. That happens with
   `configChanges` declared for smooth resizing (normal on desktop windowing), or with a multi-window or freeform resize the system does
   not relaunch for. On dispose, `rememberViewModelStoreProvider` calls `provider.clearAllKeys()` because the lifecycle is
   still ≥ CREATED (`NAV3/vmc/.../RememberViewModelStoreProvider.kt:156-157`). **Every ViewModel of every tab is
   cleared** and saveable state is lost. VitFleet hit this and fixed it: "The host is called from exactly one place,
   whatever the layout" (`VF/core/navigation/.../AppShell.kt:217-246`). PeriFlow still has three branches
   (`PF/core/navigation/.../AppShell.kt:215-325`).
2. **Auth and onboarding hand-offs swap hosts with a hard cut.** Sign-in calls `navigator.resetTo(start)`
   (`T/feature/auth/.../AuthNav.kt:44,50`) *and* flips `isAuthenticated`, which changes `entryPoint()` and makes AppRoot
   change branch. If the `ResetTo` lands first, the single-stack host starts a push into `SampleList` (ViewModel #1,
   request #1), and a frame or so later the branch swap discards it and the shell composes `SampleList` again
   (ViewModel #2, request #2) with no transition. Sign-out has the same double mechanism: the `signedOut` collectors
   (`AppRoot.kt:55-59, 70-77`) race the token-store flip (`DataStoreAuthTokenStore` is a `SessionScopedStore`,
   `T/core/datastore/.../AuthTokenStore.kt:37-44`). `_signedOut` has no replay (`SessionCoordinator.kt:31-33`), so an
   emission with no subscriber is dropped. VitFleet makes the rebuild explicit with `key(entry.identity)`
   (`VF/app/.../MainActivity.kt:133-160`).
3. **Content slides over the rail.** NavDisplay passes `sizeTransform = null` (`NavDisplay.kt:828`), so nothing
   clips. In the `Row`, the rail is drawn first and the host after it (`AppShell.kt:196-213`). The outgoing screen's -25% parallax
   on a push (and the incoming screen's start position on a pop) is drawn **over the rail**. At medium width, 25% of the content
   width is more than the rail's 88 dp (`AppNavigationRail.kt`, `Width = 88.dp`). Fix: `Modifier.clipToBounds()` on the
   host in rail mode. Not verified on a device; it follows from draw order.
4. **Scene recalculation on every StackDisplay recomposition.** `listOf(rememberListDetailSceneStrategy(),
   SinglePaneSceneStrategy())` (`AppNavHost.kt:123`) makes `remember(sceneStrategies.toList(), ...)`
   (`SceneState.kt:104`) miss every time. `ListDetailScene` has no `equals` (`ListDetailScene.kt:82-108`), so
   `LaunchedEffect(scene)` (`NavDisplay.kt:754`) restarts and runs an `animateTo` to an identical-looking scene. That briefly
   caps the scene at STARTED (`:835-841`). Fix: `remember { listOf(...) }`, and implement `equals`/`hashCode` on
   `ListDetailScene`, as PeriFlow's `SheetScene` does (`PF/.../SheetScene.kt:73-76`).
5. **Folding or unfolding plays a spurious push.** When `hasRoomForTwoPanes` flips, the same stack maps to a different
   scene key (list-detail scene vs single pane). `isPop` is false, so a push slide plays on resize.

### RC8: Bottom-bar items flash and recompose every frame *(medium, ux and performance; overlaps the design-system audit)*

- `AppBottomBar.kt:264-268`: `.selectable(selected, role = Role.Tab, onClick)` uses the default indication, which is
  `AppIndication` (`AppTheme.kt:66, 87`). On press it draws a full-bounds rect at 9% alpha (`AppIndication.kt:74-79, 83`).
  Each item is `weight(1f).fillMaxHeight()` and unclipped, so every tap flashes a grey rectangle. PeriFlow's glass bar passes
  `interactionSource = null, indication = null` (`PF/core/designsystem/.../GlassBottomBar.kt:209-215`).
- Animated values are read in composition: `Modifier.offset(x = x)` with `x by animateDpAsState`
  (`AppBottomBar.kt:162-173`), `Modifier.scale(iconScale)` (`:255-259, 284`), and three `animateColorAsState` per item
  (`:229-252`). Each frame of the pill slide recomposes the pill and both affected items, on the same frames as RC1's
  first composition. The rail has the same pattern (`AppNavigationRail.kt` `VerticalPill`, `LeadingRule`). Fix:
  `offset { IntOffset(...) }` and `graphicsLayer { scaleX = ...; scaleY = ... }` with the `State` object, and
  draw-phase colour lerp.
- `items = tabs.map { BottomNavItem(...) }` (`AppShell.kt:183-190`) builds a new list on every AppShell
  recomposition, which happens on every navigation because AppShell reads `state.current.size`. The bar therefore cannot skip.

---

## 3. PeriFlow's approach, precisely

| Commit | What changed (navigation and shell) | Measured |
|---|---|---|
| `a8aedb0` "Navigation performance: kept tabs, one shared aura, slide-only push" | `KeptTabs.kt` (new): opened tabs stay composed. The hidden ones are **measured but not placed** and their lifecycle is capped at CREATED. Tab switch is instant. `tabSwitch` was deleted. Push and pop are slide-only. | Tab switching: 12.4% → ~4% janky, p99 117 → 61 ms (Redmi Note 10 Pro) |
| `dec4ff5` "quicker tab bar return" | Bar enter and exit are `tween(quick)` over a third of the bar height plus a fade (`AppShell.kt:290-295`). The splash and system night mode follow the in-app theme (`MainActivity.kt:156-170`) | — |
| `eb2d1ee` "Back swipe tracks the finger" | `predictivePop`: a linear tween of the same slide | — |
| `84a9483`, then `8943957` | Back is held while the stack moves (`BackHold.kt`, which entries report through `ReportSceneMotion()`). **Each kept tab gets a child `NavigationEventDispatcher` that is disabled while hidden** (`KeptTabs.kt:418-435`). Tests: `BackHoldTest`, `KeptTabsBackTest` (Robolectric) | — |
| `e9dd952` | Top bar over scrolling content; `LocalTopBarInset`. The shell's separate status-bar blur was removed | Calendar scroll: 6.2% vs 5.8% janky |
| `4386048` "Glass performance" | Glass inside screens is *drawn*, not sampled. Only chrome (bar, rail, top bar) samples a recorded scene. The bottom edge blur was removed | 67% → 12.5% janky, median 61 → 22 ms |
| `d4ed283` | Tabs are not clipped (labels were cut off while the lens grows) | — |

Other PeriFlow shell pieces worth noting:

- `GlassStage` (`PF/core/designsystem/.../foundation/Glass.kt:76-96`) records only `aura + content` into one
  `layerBackdrop`. Chrome is a sibling above it, so it can sample the scene without sampling itself.
- `LocalBottomBarInset` (`Glass.kt:59-63`, used at `PF/core/navigation/.../AppNavHost.kt:161-166`): under the glass treatment,
  a root screen runs the full height *under* the bar and pads its own scroll content.
- `SystemBarIcons` (`PF/core/ui/.../PeriflowTheme.kt:36-51`) makes the status and navigation bar icons follow the
  *app's* theme.
- `SheetScene` (`PF/core/navigation/.../SheetScene.kt`) is an `OverlayScene` detent sheet as a destination, with `equals`.
- `AppLock` (`PF/app/.../lock/AppLock.kt:20-60`) uses `justUnlocked` and the own-action grace, and locks at once otherwise.

### 3.1 What remains imperfect in PeriFlow

1. **Hidden tabs are still measured** on every pass that re-measures the host (`KeptTabs.kt:404-410`: `measurable.measure(constraints)` with
   `if (active) placeable.place(0, 0)`). IME animation frames and inset or resize changes re-measure all opened tabs.
   Measure only the active one (§7.2).
2. **Infinite animations keep ticking in hidden tabs.** The CREATED cap pauses lifecycle-aware collectors, but
   `rememberInfiniteTransition` (for example `AppSkeleton.kt:43-52`) still awaits frames, so a tab hidden while loading keeps the frame
   clock running. Expose a `LocalTabVisible` and pause in the shared animated components.
3. **`opened` never shrinks** and is mutated during composition (`KeptTabs.kt:392-394`). After sign-out (`resetAll`)
   every previously opened tab stays composed. Key the kept set by session.
4. **Possible focus or IME leak** (not verified): a focused text field in a hidden tab stays attached, so the keyboard
   could stay up over the new tab. Clear focus in `select()`.
5. **AppShell still has three layout branches** (`AppShell.kt:215, 252, 283`), so a width-class change disposes all tabs (RC7.1).
6. **The bar is still tied to stack depth**, so its return after a predictive back still lags. The tween is short (150 ms), and
   the glass bar's fade still runs an alpha layer over a `drawBackdrop` node.
7. **Push and pop still settle in ~505 ms** (Fluid k = 340). `BackHold` therefore swallows real predictive gestures for half a
   second after every push (they replay as plain pops). That reads as lag. Shortening the settle (§7.4) shrinks the
   window.
8. Modal predictive back is still a spring (`predictivePopForStyle` falls back to `popForStyle` for Modal,
   `NavTransitions.kt:333-341`). VitFleet has a linear `modalPredictiveDismiss`.
9. Deep links are re-handled on every `onCreate` (`PF/app/.../MainActivity.kt:231`), and MainActivity has no
   `launchMode` (`PF/app/src/main/AndroidManifest.xml:47-50`), the same as the template (§5.1).
10. `Modifier.glass` is a `composed {}` modifier (`Glass.kt:132-171`), and `outerShadow` allocates a `Path` and a
    `Paint` on every draw (`Glass.kt:210-225`). This matters during glass-bar animations (design-system audit).
11. `rememberEntryProvider` is still rebuilt on every composition (harmless, see §4), and `items` still reallocates.

## 4. VitFleet: fixes to port, and two not to

Port these:

- A single host call site, plus `FullScreenKey` (`VF/core/navigation/.../AppShell.kt:217-246`, `AppNavKey.kt:19-23`).
- `ShellState.apply(command, rootKeys)`: navigating to another tab's root selects that tab instead of pushing a
  copy with no bar and no back arrow (`AppShell.kt:81-99`).
- `AppNavigator.applyWhileStarted(lifecycle)` (`AppNavigator.kt:66-75`), plus `android:launchMode="singleTop"`
  (`VF/app/src/main/AndroidManifest.xml:52`).
- Deep links are handled only on a fresh launch, not one replayed from Recents (`MainActivity.kt:164-169`). They wait in
  `PendingDeepLink` until the tabbed app is up (`MainActivity.kt:136-141`, `deeplink/PendingDeepLink.kt`, with a test).
- A linear predictive pop that also shrinks the leaving screen to 0.92, and a linear modal predictive dismiss (`NavTransitions.kt:59-124`).
- Saver failures are logged rather than swallowed (`AppShell.kt:150, 157`).
- `LocalCanNavigateUp` (`AppNavHost.kt:155-160`).

Do **not** port these as fixes. Each one's rationale is contradicted by the library source:

- `.underneath()` sets `targetContentZIndex = -1` (`VF/.../NavTransitions.kt:126`). NavDisplay builds a new
  `ContentTransform` with its own z-index (`NAV3/.../NavDisplay.kt:822-829`) and already lowers it on pop
  (`:673`), so this is dead code.
- The comment on the remembered entry provider (`VF/.../AppNavHost.kt:193-195`) claims a new provider rebuilds entries and
  replays a push on tab switch. Entries are rebuilt only when the stack contents change (`DecoratedNavEntries.kt:125`).
  Remembering it does no harm, but it does not fix that.

VitFleet still uses the `AnimatedContent` tab switch and the sheet-spring bar, so it still has RC1 and RC2.

---

## 5. The rest of the area

### 5.1 Deep links

1. **Re-applied on every recreation** *(medium-high, bug)*. `handleDeepLink(intent)` is called unconditionally in
   `onCreate` (`MainActivity.kt:182`). After a rotation, a theme or locale change, process-death restore, or a launch from Recents, the original
   link is pushed again on top of the restored stack. VitFleet gates it on
   `savedInstanceState == null && !FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`.
2. **Standard launch mode plus a singleton channel navigator** *(high, bug)*. MainActivity has no `launchMode`
   (`T/app/src/main/AndroidManifest.xml:44-48`). Widget taps (`actionStartActivity<MainActivity>()`,
   `widget/AppWidget.kt:69`), notification taps (`PendingIntent.getActivity`, `T/core/notification/.../AppNotifications.kt:105-113`)
   and links from other apps each start a **second MainActivity**. Both instances collect the same
   `Channel(BUFFERED).receiveAsFlow()` (`AppNavigator.kt:40-41`) from un-gated `LaunchedEffect`s (`AppNavHost.kt:56-58`,
   `AppShell.kt:171-173`). The channel fans out, so a command reaches only one of them, and roughly half of the
   navigations go to the hidden Activity. `onNewIntent` (`:219-223`) never fires with the standard mode, so its comment is wrong.
3. **Links bypass the onboarding and sign-in gates and land in the wrong tab** *(medium)*. The link is navigated into
   whatever host exists: on top of `OnboardingKey` or `SignInKey`, or onto the *selected* tab. `items` with no id
   resolves to `SampleListKey`, a tab root, which is then pushed as a non-root copy without the bar. After sign-in,
   `resetTo(start)` discards it. Fix: VitFleet's `PendingDeepLink` plus `ShellState.apply` root routing, and an owning-tab
   mapping for non-root keys (§7.6).

### 5.2 Edge-to-edge and system bars

1. **The icon colour follows the system, not the app theme** *(medium)*. `enableEdgeToEdge()` with defaults
   (`MainActivity.kt:121`) picks dark or light icons from the *system* night mode. With the app set to Dark on a light system,
   status icons are dark on dark. PeriFlow fixes it in `PeriflowTheme.kt:36-51`. The splash and window background have the
   same problem; PeriFlow's `setApplicationNightMode` (`PF/app/.../MainActivity.kt:156-170`) handles API 31+.
2. **Bottom inset only on tab roots** *(medium)*. Pushed screens and every single-stack screen draw under the
   navigation bar. `AppScaffold` applies only `imePadding()` and horizontal cutout padding (`AppScaffold.kt:46-61`). In
   `SampleDetail` (`SampleDetail.kt:95-118`) and `Licenses` (`Licenses.kt:132-151`), the last content sits behind the 3-button bar.
3. **The root inset is applied as raw padding, so the IME is double-counted.** The root `Box(...padding(bottom = inset))`
   (`AppNavHost.kt:158-163`) does not consume insets, and `AppScaffold.imePadding()` then adds the full IME height again.
   On the Search tab root, a text field in its top bar (`SearchScreen.kt:69-91`), results end a full bar height above the
   keyboard. The same raw padding makes the floating bar treatment pointless: content can never scroll under the
   floating pill and stops at a solid background band (`occupiedHeight` for Floating is 64 + 2×12 dp + nav bar, `AppBottomBar.kt:84`).
   Fix: the PeriFlow approach for every treatment. Provide the bar height as an inset (CompositionLocal, or
   `consumeWindowInsets` with an added `WindowInsets`) and let scroll containers pad their own `contentPadding`.

### 5.3 Predictive back

targetSdk is 37 (`T/build-logic/.../AppConfig.kt:14`), so predictive back is on. Beyond RC4 and RC5:

- At the root of a non-first tab, `BackHandler` (`AppShell.kt:179-181`) is non-predictive and switches tabs on release.
  That is acceptable, but it should be documented.
- `onExitRequested = { finish() }` (`MainActivity.kt:160`) is unreachable. NavDisplay's handler is disabled at the root
  (`NavDisplay.kt:558`), and the shell's `BackHandler` is only enabled when `selectedIndex != 0`. That is fortunate, because `finish()`
  on Back would defeat Android 12+'s "move task to back". Remove the parameter.
- In two-pane mode, list and detail share one scene key, so a back swipe from a detail moves nothing until release, then
  the pane swaps (`ListDetailScene.kt:89-104`).

### 5.4 App lock

1. **Lock bypass inside the grace window** *(high, security)*. `AppLock.onHidden()` stamps `lastVisibleAt` in
   MainActivity's `onStop` (`AppLock.kt:24-26`, `MainActivity.kt:212-215`). Opening the opaque `LockActivity` *stops*
   MainActivity, so the stamp is "the moment the lock appeared". Press Home on the lock screen; `noHistory` finishes
   `LockActivity` (`AndroidManifest.xml:126-132`). Return within 30 s, and `shouldLock()` sees an elapsed time under `GRACE_MILLIS` and
   returns false (`AppLock.kt:29-34`): **the app opens unlocked without authenticating**. PeriFlow's `AppLock`
   locks at once unless the app itself sent the user away (`PF/app/.../lock/AppLock.kt:41-54`). Minimal fix: a
   `pendingUnlock` flag, set when the lock is shown and cleared only by `unlocked()`, which `shouldLock()` honours first.
2. **Content shows before the lock** *(medium)*. The comment says "decided in onStart, before the window is drawn"
   (`MainActivity.kt:199-202`), but the decision is `lifecycleScope.launch { if (appLock.shouldLock()) startActivity(...) }`.
   `shouldLock` suspends on `settings.first()`, so on a warm return the last screen is drawn, then covered by an
   activity transition. Decide synchronously from a cached `@Volatile` flag that an application-scope collector keeps
   current, and draw an opaque cover in Compose until the lock is up.
3. **The lock screen ignores the generated style.** `LockActivity` calls `AppTheme(mode = ...)` with no `designStyle` or
   `motionStyle` (`LockActivity.kt:51`). The generator rewrites only `AppRoot.kt`
   (`T/../generator/genkit/render.py:328-363`). The first frame also uses `AppSettings()` defaults (`:50`), so a
   theme flash is possible. Fix: a single `BrandTheme` in `core:ui`, like PeriFlow's `PeriflowTheme`, used by both activities and written by
   the generator.

### 5.5 Session

- There are two sources of truth for where sign-out lands (RC7.2). There should be one: the token flow decides the host, and nothing
  pushes `SignInKey` into a tab.
- Each `sessionEvents.expired` emission launches a fresh `signOut()` (`SessionCoordinator.kt:44, 49-58`). A burst of
  401s runs several concurrent wipes and emissions. Use a mutex, or ignore while a wipe is running.

### 5.6 Process death and configuration changes

- The back stack and the shell round-trip as JSON through `rememberSaveable` (`AppBackStack.kt:79-99`, `AppShell.kt:114-150`).
  It works, with two silent failure modes. (a) A key registered in a `navGraph {}` but not in `navKeys {}` makes
  `encodeToString` throw, `runCatching` returns null, and **the whole stack is lost** with no log. (b) The tabs are saved by index, and
  the restore checks only `size`, so reordering tabs between versions restores stacks into the wrong tabs. Add a unit
  test that every `NavRegistry` key type is serialisable, log saver failures as VitFleet does, and key saved stacks by the root key.
- `startup` is an Activity field (`MainActivity.kt:107`). On recreation, `setContent` returns early until DataStore
  re-emits (`:147-148`), which can show a blank frame; the splash covers only a cold start. Move `Startup` into an
  Activity-scoped ViewModel `StateFlow`, which survives recreation.
- `rememberReduceMotion()` reads `TRANSITION_ANIMATION_SCALE` once, inside a `remember` (`ReduceMotion.kt:10-20`), and never
  observes it. Compose already scales every animation by the animator duration scale through `MotionDurationScale`.
  Prefer that, or observe the setting.

### 5.7 Multi-window, large screens and the rail

The rail from Medium and two panes from Expanded are good choices. Defects: RC7.1 (host disposal on a class change),
RC7.3 (content over the rail), RC7.4 and RC7.5 (scene recalculation and spurious push on fold), and the hard-cut detail swap. That swap's
rationale comment, "drawing it through a fade-out would bring a ViewModel back to life" (`ListDetailScene.kt:99-100`), does not
match 1.1.7: `onPop` waits until the entry has left composition (`DecoratedNavEntries.kt:206-221`), so a short
crossfade of the detail pane is safe. Verify on a device before relying on it.

### 5.8 DI graph

The graph is clean and small: `NavigationModule` multibinds `Set<NavGraphEntry>` and `Set<SerializersModule>`, and the app modules
(`AppModule`, `FeatureBindingsModule`, `SessionModule`, `PushTokenModule`, `DevToolsModule`) are thin. Notes:

- `AppNavigator` is a process singleton holding a channel, which is the root of §5.1.2. Either gate collection on the
  lifecycle (VitFleet), or scope command delivery to the Activity (an `@ActivityRetainedScoped` navigator).
- `AppModule` constructs a second `DevEnvironment` (`buildDevEnvironment()` in `AppModule.kt:71`) instead of injecting
  the one `DevToolsModule` provides. This is trivial.
- For scaling to many features, there is no home for keys that more than one feature opens. PeriFlow put them in
  `core/navigation/Destinations.kt` (`PF/core/navigation/.../Destinations.kt:85-183`). At scale, use `:feature:x:api` modules
  that hold keys, with registrations in `:feature:x:impl`. Also give each registered destination an owning tab, for routing (§7.6).

### 5.9 Push, Play Store, widget

- **Play update prompt loop** *(medium, bug)*. `repeatOnLifecycle(RESUMED) { appUpdates.updates().collect(::onUpdate) }`
  (`MainActivity.kt:171-176`) calls `startFlexibleUpdate` on every `Available` (`:189`). The Play dialog pauses the
  Activity, so declining it resumes the Activity, collection restarts, and the dialog is offered again. `Downloaded` calls
  `appUpdates.install(result)` (`:192`), which restarts the app under the user, contradicting the comment on the
  line above and the KDoc "never on its own" (`PlayStorePrompts.kt:45`). `ReviewPrompt.onLaunch` ("Call once per
  launch", `PlayStorePrompts.kt:62`) is called on every resume, so the launch count counts resumes.
- Widget taps and pushes start a second Activity instance (§5.1.2).
- `AppMessagingService` restricts link intents to its own package (`push/AppMessagingService.kt:243-247`), which is good.

### 5.10 Comment accuracy (AI-slop cleanup)

| File:line | Says | Reality |
|---|---|---|
| `AppNavHost.kt:28` | "the *only* file in the project that names Navigation 3" | `ListDetailScene.kt:14-17` imports `androidx.navigation3.*` |
| `NavTransitions.kt:17` | "Three shapes, and only three" | push, pop, modal, modalDismiss, fade, tabSwitch, none |
| `AppNavigator.kt:43` | "Not `suspend`, unlike the obvious design." | Gives no reason (the reason: callable from non-suspending UI callbacks, buffered) |
| `AppRoot.kt:29-30` | "the two app-wide reactions" | There is one (`signedOut`) |
| `MainActivity.kt:190-192` | "Asking rather than restarting under the user" | The next line restarts |
| `MainActivity.kt:199-202` | "never on screen — even for a frame" | The decision is asynchronous (§5.4.2) |
| `MainActivity.kt:221` | "A link that arrives while the app is running comes here" | Not with the standard launch mode |
| `BaselineProfileGenerator.kt:28` | "Scrolling is included" (line 28) | The journey only calls `waitForIdle()`, and nothing navigates (§5.11) |

### 5.11 Measurement gap

`T/benchmark/.../BaselineProfileGenerator.kt:21-30` profiles only startup. No tab switch, push, pop or back gesture is
in the baseline profile, so NavDisplay, the animation and transition code, and the bar run interpreted or JIT the first
time on a fresh install. That is the worst-case first impression. Only `StartupBenchmark` exists, with no `FrameTimingMetric`. Add a
navigation journey to the profile, and a `FrameTimingMetric` macrobenchmark for tab switch ×N, push/pop ×N and
back swipe ×N. That turns PeriFlow's manual `gfxinfo` numbers into a regression gate.

---

## 6. Liquid glass (Kyant backdrop) in the shell: optional and fast

The user asked for the Kyant liquid-glass library (`io.github.kyant0:backdrop:2.0.1`, `shapes:1.2.1`;
`PF/gradle/libs.versions.toml:43-44, 78-79`) to be included and optimised. The shell decides most of glass's cost,
so it belongs in this audit as an **opt-in feature** (`// <opt:glass>`). PeriFlow's measured rules:

1. **One recorded scene per window, chrome only.** `GlassStage` (`Glass.kt:76-96`) records `aura + content` into one
   `layerBackdrop`. Only chrome samples it: the tab bar, rail and condensed top bar (`GlassDepth.Chrome`). Glass inside
   screens is drawn: `drawnGlass`, a translucent body, a sheen gradient, a rim and an outer shadow (`Glass.kt:178-203`). The
   measured result went from 67% to 12.5% janky (`4386048`).
2. **No alpha on anything that contains or covers glass.** Push and pop are slide-only (`a8aedb0`). The template's push
   fade (RC3), tab cross-fade (RC1) and bar fade (RC2) each turn a blur-plus-lens pass into an offscreen composite on every frame.
3. **Tabs are kept, and the switch is instant**, so the lens glide is the motion (`a8aedb0`).
4. **Content runs under the bar.** Use `LocalBottomBarInset` instead of padding (`PF/.../AppNavHost.kt:161-166`), so there is
   something to refract. The template's root padding (§5.2.3) would leave the glass over a flat background band.
5. **The lens samples the screen only while pressed**: `backdrop = if (lensPressed) combinedBackdrop else tabsBackdrop`
   (`GlassBottomBar.kt:253-256`). The invisible accent copy of the tabs has no backdrop of its own (`:220-223`).
6. **No ripple**: `indication = null` on tabs (`GlassBottomBar.kt:209-215`).
7. **A fallback that keeps text readable**: `isRenderEffectSupported()` (API 31+) or a missing stage falls back to a near-opaque frosted body
   (`Glass.kt:149-155`, `GlassBottomBar.kt:94-102`).

Further optimisations for the generator version, beyond PeriFlow:

- Remove the bar's fade entirely (§7.3). Under a persistent or root-following bar, `drawBackdrop` never runs under an alpha layer.
- Replace `Modifier.glass`'s `composed {}` with a `Modifier.Node`, and cache the `Path` and `Paint` in `outerShadow` with
  `drawWithCache` (`Glass.kt:210-225` allocates on every draw).
- `GlassStage` wraps content in a recorded layer even when nothing samples it (no glass chrome on a
  non-root screen when the bar is hidden). That costs little (a `RenderNode` re-record), but in rail and wide layouts without glass
  chrome, do not install the stage at all.
- Keep `lens()` for API 33+ and blur only for API 31–32 (Kyant gates the shader internally; check it on an API 31 device).
- Gate the glass bar's spring settles on visibility thresholds (PeriFlow `a8aedb0`, "settle at half a pixel").

---

## 7. Recommended target design, with code sketches

### 7.1 Shell: one host call site, chrome as overlay, clipping in rail mode

```kotlin
@Composable
fun AppShell(tabs: List<ShellTab>, state: ShellState, registry: NavRegistry, navigator: AppNavigator, modifier: Modifier = Modifier) {
    val wide = AppTheme.windowSize.width >= WindowWidth.Medium
    val fullScreen = state.top is FullScreenKey                 // VitFleet
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val rootKeys = remember(tabs) { tabs.map { it.key } }
    LaunchedEffect(navigator, state, lifecycle) {
        navigator.applyWhileStarted(lifecycle) { state.apply(it, rootKeys) }   // VitFleet
    }
    val items = rememberBottomNavItems(tabs)                     // remembered, not rebuilt per navigation
    val bar = remember { BarAnchor() }

    ShellChrome(                                                 // GlassStage when <opt:glass>, plain Box otherwise
        modifier = modifier.fillMaxSize(),
        chrome = {
            if (!wide) AppBottomBar(items, state.selectedIndex, state::select,
                Modifier.align(Alignment.BottomCenter).followRoot(bar, hidden = fullScreen))
        },
    ) {
        Row(Modifier.fillMaxSize()) {
            if (wide && !fullScreen) AppNavigationRail(items, state.selectedIndex, state::select)
            CompositionLocalProvider(LocalBarAnchor provides bar, LocalBottomBarInset provides barInset(wide, fullScreen)) {
                TabbedNavHost(state, registry, onBack = { state.handleBack() },
                    modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds())   // RC7.3
            }
        }
    }
}
```

### 7.2 KeptTabs v2: instant switch, only the active tab measured, hidden tabs paused

Start from `PF/core/navigation/.../KeptTabs.kt` and keep `TabLifecycle` and `TabBackDispatcher` unchanged. Change three things:

```kotlin
Box(
    Modifier.layout { measurable, constraints ->
        if (!active) return@layout layout(constraints.maxWidth, constraints.maxHeight) {}   // not measured, not placed
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    },
) {
    CompositionLocalProvider(LocalTabVisible provides active) { content(index) }   // shimmer, Lottie and similar pause on this
}
```

- `ShellState.select()` clears focus (`LocalFocusManager.clearFocus()` through a callback) before switching.
- Key the kept set by a session generation, so `resetAll()` on sign-out drops the composed tabs.
- Verify on a device that skipping `measure` for an inactive child is accepted by the version of Compose UI in use.
  If it is not, fall back to PeriFlow's measure-everything behaviour.

### 7.3 Bar behaviour

- **Default: the bar stays visible at every depth**, as on iOS by default. Every entry receives `LocalBottomBarInset`. There is no
  `AnimatedVisibility` and no bar motion during push or pop, which removes RC2.
- **`FullScreenKey` destinations**: the bar *follows the root screen* in the draw phase only:

```kotlin
@Stable class BarAnchor { var rootOffsetX by mutableFloatStateOf(0f) }

// Entry wrapper, root entries only: report the root's horizontal displacement while it moves.
Modifier.onGloballyPositioned { anchor.rootOffsetX = it.positionInRoot().x - hostLeft }

// Bar: translate with the root, and clip at the incoming screen's leading edge so it reads as covered.
fun Modifier.followRoot(anchor: BarAnchor, hidden: Boolean, parallax: Float = 0.25f) = graphicsLayer {
    translationX = anchor.rootOffsetX
    // incoming edge = W * (1 + rootOffsetX / (parallax * W)) for push and pop (same progress drives both)
    val edge = size.width * (1f + anchor.rootOffsetX / (parallax * size.width))
    clip = true; shape = RightClipShape(edge.coerceIn(0f, size.width))
    alpha = if (hidden && anchor.rootOffsetX == 0f) 0f else 1f
}
```

This tracks pushes, pops and predictive back with no extra animation and no recomposition. Validate it on a device. It is the
opt-in "hides bar when pushed" behaviour, not the default.

### 7.4 Transitions

```kotlin
// AppMotion: every full-screen move is critically damped and settles in about 350 ms
fun <T> screen(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = screenStiffness)   // 600..700 per style

fun push(m: AppMotion) = {
    slideIntoContainer(Start, m.screen()) togetherWith
        slideOutOfContainer(Start, m.screen(), targetOffset = { (it * m.outgoingParallax).toInt() })
}
fun pop(m: AppMotion) = { /* mirror, no alpha */ }
fun predictivePop(m: AppMotion) = {                                   // PeriFlow + VitFleet
    val track = tween<IntOffset>(m.slow, easing = LinearEasing)
    slideIntoContainer(End, track, initialOffset = { (it * m.outgoingParallax).toInt() }) togetherWith
        (slideOutOfContainer(End, track) + scaleOut(tween(m.slow, easing = LinearEasing), targetScale = 0.94f))
}
fun modal(m: AppMotion) = { slideInVertically(m.screen()) { it } togetherWith scaleOut(tween(m.medium), 0.96f) }   // no fade, no overshoot
```

- Delete `tabSwitch`, `TAB_ENTER_SCALE`, `outgoingDim` (no longer used) and, if no feature uses it, the `Fade` style.
- Wrap entries in `HoldBackWhileMoving` (PeriFlow `BackHold.kt`, with tests). With a ~350 ms settle, the hold window shrinks
  by about a third.
- Optional depth cue: a 10 dp horizontal gradient drawn in `drawBehind` on the incoming entry's leading edge. It is not an alpha layer.

### 7.5 Single source of truth for the entry point

- Remove `navigator.resetTo(...)` from auth and onboarding completion. The token and settings flows decide the host.
- Keep `key(entry.identity) { ... }` (VitFleet) and wrap it in one `AnimatedContent(targetState = identity)` with a
  short crossfade, the only place a whole-host fade is acceptable because it happens once per session.
- Remove the per-branch `signedOut` collectors from `AppRoot`. Sign-out clears the token, and the identity changes.
- Move `Startup` into a ViewModel, so recreation has it synchronously.

### 7.6 Deep links and cross-tab routing

```kotlin
// MainActivity
if (savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) holdLink(intent)
override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); holdLink(intent) }  // launchMode=singleTop

// Shell (signed in): take the waiting link once
LaunchedEffect(Unit) { pendingLinks.waiting.filterNotNull().collect { pendingLinks.take()?.let(state::open) } }

// ShellState.open: a root key selects its tab; another key selects its owning tab (NavRegistry metadata) and pushes
```

### 7.7 Lock, system bars, theme

- `AppLock`: add a `pendingUnlock` flag, and adopt PeriFlow's `justUnlocked` and own-action grace. Make the decision synchronous from cached settings.
- `SystemBarIcons(dark = !AppTheme.colors.isLight)` from PeriFlow, inside the one `BrandTheme`, plus
  `setApplicationNightMode` on API 31+.
- `LockActivity` uses `BrandTheme`. The generator writes `BrandTheme`, not `AppRoot`.

---

## 8. Findings index

| # | Sev | Category | Location | Finding |
|---|---|---|---|---|
| 1 | high | performance | `T/core/navigation/.../AppNavHost.kt:99-106` | Tab switch disposes and rebuilds the tab and cross-fades two full-screen layers |
| 2 | high | ux-design | `AppShell.kt:175-176, 227-233` | Bar visibility follows stack depth on its own vertical spring |
| 3 | high | bug | `AppNavHost.kt:126-129` | Predictive back scrubs a spring |
| 4 | high | bug | `AppNavHost.kt:109-131` | No guard for a back gesture during a push |
| 5 | high | security | `T/app/.../lock/AppLock.kt:24-34` | Lock bypass inside the grace window |
| 6 | high | bug | `T/app/src/main/AndroidManifest.xml:44-48` | Standard launch mode and singleton channel split commands across Activity instances |
| 7 | medium | bug | `MainActivity.kt:182` | Deep link re-applied on every recreation |
| 8 | medium | bug | `MainActivity.kt:225-228` | Links bypass gates and land in the wrong tab |
| 9 | medium | performance | `NavTransitions.kt:30-33, 43-46` | Alpha fade on push and pop |
| 10 | medium | bug | `NavTransitions.kt:59-62` | Modal overshoot gap; Bouncy navigation overshoot (`AppMotion.kt:55`) |
| 11 | medium | architecture | `AppShell.kt:195-225` | Width-class branch disposes all tabs and clears ViewModels |
| 12 | medium | ux-design | `AppShell.kt:196-213` | Content slides over the rail |
| 13 | medium | architecture | `T/app/.../ui/AppRoot.kt:52-86` | Auth hand-off swaps hosts and runs racy duplicate mechanisms |
| 14 | medium | ux-design | `MainActivity.kt:121` | System bar icons and splash ignore the in-app theme |
| 15 | medium | ux-design | `AppNavHost.kt:153-168` | Bottom inset only on roots; IME double-counted; no content under the floating bar |
| 16 | medium | ux-design | `AppBottomBar.kt:264-268` | Tab press flash and per-frame recomposition |
| 17 | medium | bug | `MainActivity.kt:171-195` | Play update prompt loop and unasked restart |
| 18 | medium | security | `MainActivity.kt:199-210` | Lock decided asynchronously; LockActivity off-style |
| 19 | medium | measurement | `T/benchmark/.../BaselineProfileGenerator.kt:21-30` | No navigation in the baseline profile and no frame benchmark |
| 20 | low | performance | `AppNavHost.kt:123` | Scene strategies not remembered; `ListDetailScene` has no `equals` |
| 21 | low | ux-design | `ListDetailScene.kt:89-104` | Two-pane hard cut, no predictive feedback, spurious push on fold |
| 22 | low | bug | `AppBackStack.kt:86-97` | Saver failures swallowed; stacks keyed by index |
| 23 | low | bug | `MainActivity.kt:107` | Startup in an Activity field gives blank frames on recreation |
| 24 | low | bloat | `AppShell.kt:164` | Dead API and parameters |
| 25 | low | ai-slop | §5.10 | Inaccurate comments |
| 26 | low | anti-pattern | `ReduceMotion.kt:10-20` | Reduce-motion read once and duplicates `MotionDurationScale` |
| 27 | low | bug | `SessionCoordinator.kt:44-58` | Concurrent sign-outs; emissions dropped |
| 28 | medium | performance | `PF/core/navigation/.../KeptTabs.kt:392-410` | PeriFlow KeptTabs: hidden tabs measured and animating, never released |
| 29 | low | anti-pattern | `VF/core/navigation/.../NavTransitions.kt:126` | VitFleet `.underneath()` is dead under NavDisplay |

## 9. Verification plan, for the implementation phase

1. Robolectric: port `KeptTabsBackTest` and `BackHoldTest`. Add tests for `ShellState.apply` root routing, deep-link
   hold-and-take, and an `AppLock` test for the "Home on lock screen, return within grace" case.
2. A unit test that every `NavRegistry` key type encodes and decodes with `NavKeySerialization` (catches silent stack loss).
3. Macrobenchmark with `FrameTimingMetric`: 20 tab switches, 20 push/pop pairs and 10 back swipes on the generated full spec. Report
   P50, P90 and P99 frame duration and the janky percentage. Add the same journey to `BaselineProfileGenerator`.
4. Manual checks on a device: predictive back at 30% (the screen should be about 30% out), back during a push, a fold or freeform resize
   across 600 dp (the tabs keep their state), widget tap while the app is open (one Activity), rotation after opening a link
   (no duplicate), dark app on a light system (icons readable).
