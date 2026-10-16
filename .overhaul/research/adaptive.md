# Adaptive UI: best-in-class implementation and template enforcement

Research for requirement 23 (with 22, 25 and 26), 2026-10-09. Scope: Android template (Compose 1.12.1 / BOM
2026.09.00, foundation-only design system, Navigation 3 1.2.0, AGP 9.4.1, Kotlin 2.4.20, minSdk 26, targetSdk 37), the
Compose Multiplatform template (WP07, not started) and the SwiftUI template (WP08, no Swift written yet).

Evidence tags:
- **[V]** I checked it today in library source or bytecode in `~/.gradle/caches`, in androidx or JetBrains source on
  GitHub, in Maven module metadata, or on a primary docs page.
- **[S]** From a secondary source (a blog post or a third-party transcript index).
- **[U]** Not verified. It is an assumption, or it needs a device run.
- **[D]** A design decision of ours, with the reason given.

Code under review: the WP04 worktree (`wt/WP04/templates/android`, which holds the shell and the bar) and the WP03
worktree (`wt/WP03/templates/android`, which holds the design system). Paths below are relative to
`templates/android/` in the worktree named.

---

## 0. Ranked summary (most important first, for this project)

| # | What | Why it ranks here | Owner |
|---|---|---|---|
| 1 | **Gate the window size by class.** Today the exact-dp `AppWindowSize` is a *static* CompositionLocal, so every dp of a resize recomposes the whole app without skipping. Provide classes, not pixels, and give each pane its own scoped class. | This is a perf defect in code that already exists. On CMP-iOS, every frame of a rotation or Stage Manager resize triggers it, because iOS animates `containerSize` [V]. It is also the base that every later item builds on. | WP03 (+WP07 port) |
| 2 | **Make the app safe to run as several windows, and stop recreating it on size changes.** `AppNavigator` and `PendingDeepLink` are `@Singleton`, so with two windows a tap in one navigates the other. Declare `configChanges` for size, keyboard and navigation changes. Add the multi-instance property. | A correctness bug in desktop windowing, in split-screen with two instances, and on Googlebook. Tier 3 `Keyboard_Input` requires no relaunch when a keyboard is attached. | WP04 + WP02 |
| 3 | **Navigation chrome: bar → rail → sidebar** (iPadOS 18 parity). One `ShellTab` model drives all three, with no host rebuild. The rail overflow and fixed-height defects get fixed. The sidebar is the default from Large width and opt-in at Expanded. A toggle animates transforms only. | Requirement 23 names it. The rail clips at 6 or more tabs, and at font scale 2 [V, code]. | WP04 |
| 4 | **The pane system.** Use one `NavPane` metadata API for list-detail, supporting pane, the extra (third) pane at Large and wider, and hinge-aware splits. In two panes, Back and predictive back act on the detail pane only, the list shows the selected row, the detail drops its Back arrow, and a placeholder fills the empty pane. | Requirement 23 calls these canonical layouts. The current `ListDetailScene` is only half of this. | WP04 |
| 5 | **Enforcement.** Make the lint ids errors. Add a `verifyAdaptiveRules` build task, the same pattern as `ModuleGraph`. Add `@AppScreenPreviews` multi-size previews. Add a Roborazzi screenshot matrix generated from previews, plus golden-free *adaptive audit* tests. Add resize/continuity tests and emulator scripts, and a perf run of large layouts on the Redmi using `wm density`. | Requirement 23 says the template must *force* adaptivity. Golden-free checks work in generated projects, whose colours and fonts differ from the template's. | WP04/WP03 tests, WP02 build-logic, WP09 matrix |
| 6 | **Agent docs and an override note for the vendored skill.** The vendored Google `adaptive` skill tells agents to use `NavigationSuiteScaffold` (this pulls in material3 [V]), Material's `ListDetailSceneStrategy`, the experimental `Grid`, the global `mediaQuery` flag and Google's screenshot plugin. All five conflict with the template. | Requirement 26 vendors that skill into every project, so without an override agents will break the no-material3 rule. Cheap to fix. | WP12 |
| 7 | **Input parity.** Fix the bug where a right-click fires `onClick`. Add hover states and pointer icons, a right-click context menu (the same `AppMenu` as long-press), keyboard shortcuts plus the Shortcuts Helper, Esc and Enter rules, focus restore, scroll indicators for mouse users, Ctrl+wheel zoom in the image viewer, drag and drop between windows, and stylus handwriting in fields (already on [V]). | Tier 2 and desktop quality items. The right-click bug is a defect in the code today [V]. | WP03 (+WP04 shell shortcuts) |
| 8 | **Components that adapt themselves.** Sheets become a centred form sheet at regular width and height. Lists and sections keep a readable width while the window scrolls everywhere. Menus, toasts, alerts and the image viewer get rules per class. Compact height (a phone held sideways) gets its own rules. | Today most components only cap their width [V, code]. | WP03 |
| 9 | **Foldables.** A single posture and hinge source per Activity. Tabletop splits the image viewer at the fold and puts the sheet's medium detent on the fold. Book posture splits list-detail at the hinge. Fold and unfold keep tabs and state. | Requirement 23 names foldables. They are cheap once #1 and #4 exist. | WP03/WP04 |
| 10 | **CMP parity.** `AppWindowSize` in `commonMain` (androidx `window-core` 1.5.1 publishes iOS artifacts [V]). Posture and input traits as `expect`/`actual`. iPad Info.plist rules. Roborazzi `androidHostTest` for the previews. | WP07 has not started; the design should be settled now. | WP07 |
| 11 | **SwiftUI parity.** `TabView` with `.sidebarAdaptable` (iOS 18+, iOS 17 fallback), `NavigationSplitView` for list-detail, `.inspector` for the supporting pane, geometry-based width classes rather than `horizontalSizeClass` alone, keyboard shortcuts, hover and context menus, and iPadOS 26/iOS 27 resizability checks. | WP08 has not written Swift yet. Apple's iOS 27 rules make resizing mandatory. | WP08 |

Measured numbers: none yet. Nothing adaptive has been measured on the device. §8 defines the scenarios and the gates
to add.

---

## 1. Platform facts (verified today)

### 1.1 Window size classes
- `androidx.window:window-core` 1.5.1, `WindowSizeClass` [V, source `WindowSizeClass.kt`]:

  | Constant | Value |
  |---|---|
  | `WIDTH_DP_MEDIUM_LOWER_BOUND` | 600 |
  | `WIDTH_DP_EXPANDED_LOWER_BOUND` | 840 |
  | `WIDTH_DP_LARGE_LOWER_BOUND` | 1200 |
  | `WIDTH_DP_EXTRA_LARGE_LOWER_BOUND` | 1600 |
  | `HEIGHT_DP_MEDIUM_LOWER_BOUND` | 480 |
  | `HEIGHT_DP_EXPANDED_LOWER_BOUND` | 900 |

  `BREAKPOINTS_V2` adds Large and Extra-large; heights are the same in V1 and V2.
- `window-core` 1.5.1 is published for `iosArm64` and `iosSimulatorArm64` as well as Android and JVM [V, Google Maven
  metadata]. The template's `AppWindowSize.kt` can therefore move to CMP `commonMain` unchanged.
- Device shares per class, from the official page [V,
  https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes]:
  - Compact width covers 99.96 % of phones in portrait.
  - Medium width covers 93.73 % of tablets in portrait.
  - Expanded width covers 97.22 % of tablets in landscape.
  - Large width means large tablets; Extra-large means desktop.
  - Compact height covers 99.78 % of phones in landscape.

  The page's guidance: decide mostly on width; also use height for phones in landscape; never decide by device type.
- Material's pane directive [V, androidx `PaneScaffoldDirective.kt`]:
  - Panes per width: Compact 1, Medium 1, Expanded 2, Large and Extra-large 3.
  - Spacer: 0 dp below Expanded, 24 dp from Expanded.
  - Default pane width: 360 dp, and 412 dp at Large and wider.
  - Two vertical partitions in tabletop posture, or when there is a single column and the height is Expanded.
  - Vertical hinges are excluded by default (`HingePolicy.AvoidSeparating`).

  The template's `listPaneWidth` tokens (360 and 412) already match.
- iPad sizes in points: 11" portrait 834 (Medium), 11" landscape 1194 (Expanded), 13" landscape 1376 (Large). Stage
  Manager allows any window size [U, from memory; check against Apple's HIG device table].

### 1.2 Android 16 and 17 rules on orientation and resizability (the template targets 37)
- Android 16 (API 36), on displays with smallest width of 600 dp or more [V,
  https://developer.android.com/about/versions/16/behavior-changes-16]:
  - Ignored manifest attributes: `screenOrientation`, `resizeableActivity`, `minAspectRatio`, `maxAspectRatio`.
  - Ignored runtime calls: `setRequestedOrientation()` and `getRequestedOrientation()`.
  - Exceptions: games (`android:appCategory`), displays narrower than sw600, and the user's per-app aspect-ratio
    override.
  - The temporary opt-out property `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` no longer applies
    at API 37.
- Android 17 (API 37) removes that opt-out. Play requires target 37 for new apps and updates by **August 2027** [V,
  https://developer.android.com/blog/posts/prepare-your-app-for-the-resizability-and-orientation-changes-in-android-17].
  - Issues the post lists: stretched UI, controls pushed off screen, camera previews, and state lost when size changes
    recreate the activity.
  - Its suggested tests: `UNIVERSAL_RESIZABLE_BY_DEFAULT` compat flag, Compose UI Check, `DeviceConfigurationOverride`.
- The template already fails the build on lint `LockedOrientationActivity`, `SourceLockedOrientationActivity` and
  `NonResizeableActivity` (`build-logic/.../AndroidCommon.kt:47-51`, WP04) [V, code]. That is good; §6.1 extends it.

### 1.3 Material adaptive libraries without material3
Dependency sets of the 1.3.0 artifacts [V, Google Maven `.module` files]:

| Artifact | Depends on material3? | Notes |
|---|---|---|
| `androidx.compose.material3.adaptive:adaptive` | no | foundation, ui, `androidx.window:window`, `window-core` |
| `…:adaptive-layout` | no | animation, foundation, ui, `adaptive`, core, `window-core` |
| `…:adaptive-navigation` | no | `adaptive-layout`, activity-compose |
| `…:adaptive-navigation3` | no | `adaptive-navigation`, `navigation3-ui` (≥1.0.0), `navigationevent-compose` |
| `androidx.compose.material3:material3-adaptive-navigation-suite` (1.4.0) | **yes** (`material3:material3`) | `NavigationSuiteScaffold` is forbidden here |

Releases [V, https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive]:
- 1.3.0 (2026-08-12): `adaptive-navigation3` with `ListDetailSceneStrategy` and `SupportingPaneSceneStrategy`, plus
  Large and Extra-large classes.
- 1.4.0-alpha03 (2026-10-07).
- 1.2.0 added reflow and levitate strategies and `PaneExpansionState`.

**[D] Decision: do not adopt Material's scene strategies. Keep and extend the template's own `ListDetailScene`.**
- Material's scenes bring Material pane motion (`AnimatedPane`) and their own 24 dp spacing; the official recipe
  overrides that to 0 for b/418201867.
- Their back handling is separate from the template's `BackGate`, `KeepUnderScene`, `sameStack` and Gmail-style
  predictive back, which WP04 has already integrated and measured.
- What we copy from Material are its *numbers*: pane counts, the 360/412 dp widths and the hinge policy.

Optional: `adaptive` (core) is cheap, because `androidx.window:window` already arrives through Compose UI. It is still
not needed (§2.1).

### 1.4 `mediaQuery` (Compose UI 1.12.1)
- Present in ui 1.12.1 under `@ExperimentalMediaQueryApi`, and still experimental in 1.13.0-alpha03 [V, bytecode].
- It needs the global `ComposeUiFlags.isMediaQueryIntegrationEnabled = true`; the flag is still present in
  1.13.0-alpha03. Without the flag, `LocalUiMediaScope` throws "not present" [V, source].
- `UiMediaScope` exposes `windowWidth` and `windowHeight` (`@FrequentlyChangingValue`), `windowPosture`
  (Flat/Tabletop/Book), `pointerPrecision` (Fine/Coarse/Blunt/None), `keyboardKind` (Physical/Virtual/None),
  `hasCamera`, `hasMicrophone` and `viewingDistance`.
- Android implementation [V, `MediaQuery.android.kt`, `ComposeViewContext.android.kt:508`]:
  - It is created **per `AndroidComposeView` owner**. Every Popup and Dialog has its own owner.
  - Each owner collects `WindowInfoTracker.windowLayoutInfo` and registers an `InputDeviceListener` (which enumerates
    devices over binder).
  - Each owner adds an `OnGlobalLayoutListener` that runs on every layout pass, and registers a sticky
    `ACTION_DOCK_EVENT` receiver.
  - So each popup opened with the flag on pays those registrations in its first frame [U, magnitude unmeasured].
- The size it sees comes from the owner's `windowInfo`, not `LocalWindowInfo`. As a result
  `DeviceConfigurationOverride.WindowSize` in tests does **not** reach `mediaQuery { windowWidth }` [V, source].
- JetBrains `compose-multiplatform-core` `jb-main` has **no** iOS, desktop or skiko implementation of `UiMediaScope`;
  only Android and web exist [V, repo tree]. On iOS, `mediaQuery` would throw.
- **[D]** For these reasons the template provides its own traits (§2.1), one per Activity or window, and does not set
  the global flag.

### 1.5 Grid and FlexBox
- Both are in foundation-layout 1.12.1 under `@ExperimentalGridApi` and `@ExperimentalFlexBoxApi`. In 1.13.0-alpha03
  those annotation classes are gone, which suggests they go stable in 1.13 [V, bytecode; "stable in 1.13" is U].
- **[D]** Use neither on 1.12. Adopt `Grid` for dashboard-style screens when the BOM moves to Compose 1.13 stable.
  Until then use `LazyVerticalGrid(GridCells.Adaptive(...))` or a custom `Layout`.

### 1.6 Navigation 3 1.2.0 scene APIs [V, `navigation3-ui-android-1.2.0` bytecode]
- `Scene` (`key`, `entries`, `previousEntries`, `content`, default `metadata`), `SceneStrategy`, `SceneStrategyScope`
  (`onBack`), `SinglePaneSceneStrategy`, `OverlayScene` and `DialogSceneStrategy`.
- **`SceneDecoratorStrategy`** with `decorateScene(scene)`, and a `NavDisplay` overload that takes
  `sceneDecoratorStrategies` and a `SharedTransitionScope`.
- **`SharedEntryInSceneNavEntryDecorator`** (`rememberSharedEntryInSceneNavEntryDecorator(sharedTransitionScope)`) and
  `LocalCurrentScene`.
- The recipes use `NavMetadataKey<T>` and `metadata { put(...) }`. The template still uses string metadata keys
  (`PANE_METADATA_KEY`).
- Google's `navscenedecorator` recipe puts the bar and rail **inside** scenes, using movable content and
  `sharedElement` [V, android/nav3-recipes]. **[D] Do not copy that here.** The template keeps its chrome at the shell
  level, which is cheaper (no shared element per transition) and matches KeptTabs and the floating bar's behaviour.

### 1.7 Adaptive app quality tiers (IDs as Google writes them) [V]
- **Tier 3, Adaptive ready** (https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-3):
  - Configuration: `Config_Changes` (fill the display; keep scroll, text and playback across rotation, fold and
    resize) and `Config_Combinations`.
  - Multi-window: `Multi-Window_Functionality`, `Multi-Resume`.
  - Media: `Camera_Preview`, `Media_Projection`.
  - Input: `Keyboard_Input` (switch between physical and virtual keyboards **without relaunch**),
    `Mouse_Trackpad_Basic`, `Stylus_Basic`, `Stylus_Text_Input`.
- **Tier 2, Adaptive optimized** (`…/tier-2`):
  - Layout: `Responsive_adaptive_layouts` (by window size class), `UI_Secondary_Elements`, `Touch_Targets` (48 dp),
    `Drawable_Focus`.
  - Keyboard: `Keyboard_Navigation` (Tab and arrow keys), `Keyboard_Shortcuts`, `Keyboard_Media_Playback`,
    `Keyboard_Send` (Enter), `Keyboard_Exit` (Esc).
  - Pointer: `Context_Menus` (right-click), `Content_Zoom` (Ctrl+wheel, pinch), `Hover_States`.
- **Tier 1, desktop** (`…/experiences/desktop`):
  - UI: `Scrollbar_Display`, `Hover_Parity`, `Desktop_Menus`, `UI_Config`, `Request_Fullscreen_Mode`.
  - Keyboard and pointer: `Keyboard_Parity`, `Input_Combinations` (Ctrl+click, Shift+click), `Triple_Click`.
  - Windows: `Multi-Instance`, `Drag_Drop_Support`, `Drag_Drop_Batch`.
  - Cursors: `Custom_Cursors`, `Cursor_Target_Size`.
  - Plus file and print items.
- **Tier 1, foldables** (`…/experiences/foldables`): `Foldables_Postures` (tabletop for video and calls, book for
  reading), `Foldables_Camera`, `Foldables_Multi-Instance`.
- Recommended test devices: Googlebook (160 ppi), foldable 841×701 dp, 8" tablet 1024×640 dp, 10.5" tablet
  1280×800 dp, 13" Chromebook 1600×900 dp [V, overview page].

### 1.8 Desktop windowing, Googlebook and multi-instance [V, https://developer.android.com/develop/ui/compose/layouts/adaptive/support-desktop-windowing]
- Multi-instance system UI (API 35+):
  `<property android:name="android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI" android:value="true"/>`.
  Open a new window with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK | FLAG_ACTIVITY_LAUNCH_ADJACENT`.
- Drag and drop between windows: `Modifier.dragAndDropSource { … }` with `DRAG_FLAG_GLOBAL_SAME_APPLICATION` and
  `DRAG_FLAG_START_INTENT_SENDER_ON_UNHANDLED_DRAG` (Android 15+). Receive with `Modifier.dragAndDropTarget` and call
  `requestDragAndDropPermissions`.
- Header (caption) bar:
  - `APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND`, `APPEARANCE_LIGHT_CAPTION_BARS`.
  - `WindowInsets.captionBar` and `WindowInsets.isCaptionBarVisible`.
  - `getBoundingRects` gives the system-button areas to avoid.
  - Interactive UI drawn in the header needs `setSystemGestureExclusionRects`.
- Precise pointer: `WindowInfoTracker.windowEngagementInfo(activity)` with `EngagementMode.PRECISE_POINTER`. This is
  in **window 1.6.0-alpha05** (2026-06-17), not stable. Launch bounds use `ActivityOptions.setLaunchBounds`.
  `Activity.requestFullScreenMode()` is also available.
- Googlebook is an Android-based laptop line. Google points to the desktop emulator in Studio Canary and to "scene
  decorators that wrap screens with persistent desktop navigation rails" [V, blog
  https://android-developers.googleblog.com/2026/09/adaptive-development-scale-app-googlebook.html].
- Configuration changes [V, https://developer.android.com/guide/topics/resources/runtime-changes]:
  - `android:configChanges="screenSize|smallestScreenSize|orientation|screenLayout"` stops recreation for size
    changes, and "your Compose UI automatically recomposes".
  - Don't use it as a shortcut for state loss: process death and other configuration changes still recreate.

### 1.9 iPadOS 26 and iOS 27 [V, TN3192 https://developer.apple.com/documentation/technotes/tn3192-migrating-your-app-from-the-deprecated-uirequiresfullscreen-key]
- `UIRequiresFullScreen` is deprecated from iPadOS 26. From iOS and iPadOS 27 it no longer opts the app out of
  resizing; the system resizes the scene *discretely* instead.
- A launch screen is required for App Store submission from iOS 27.
- The app must support all interface orientations.
- Use `UISceneSizeRestrictions.minimumSize`. In SwiftUI use `.windowResizability(.contentMinSize)` and
  `onInteractiveResizeChange(_:)` (iOS 26).
- Orientation lock is per view controller and only a preference: `prefersInterfaceOrientationLocked`.
- WWDC26 session 278 [S, transcript index https://nonstrict.eu/wwdcindex/wwdc2026/278/]:
  - iPhone apps are resizable under iPhone Mirroring on the Mac and on an iPad's external display.
  - The UIScene lifecycle is required with the latest SDKs; without it the app no longer launches.
  - iPhone apps can opt into sidebars through `sidebar.preferredPlacement` (UIKit).
- `horizontalSizeClass` stays `.compact` for iPhone apps however wide the resizable window gets, so decide from
  geometry instead [S, https://fatbobman.com/en/posts/from-size-class-to-available-space/].
- SwiftUI availability [V, Apple docs JSON]:

  | API | iOS |
  |---|---|
  | `sidebarAdaptable`, `tabViewSidebarHeader` | 18.0 |
  | `NavigationSplitView` | 16.0 |
  | `NavigationSplitView(preferredCompactColumn:)` | 17.0 |
  | `inspector(isPresented:)` | 17.0 |
  | `onKeyPress` | 17.0 |
  | `hoverEffect(_:isEnabled:)` | 17.0 |
  | `contextMenu(menuItems:preview:)` | 16.0 |
  | `dropDestination(for:)` | 16.0 |
  | `onGeometryChange` | 16.0 |
  | `containerRelativeFrame` | 17.0 |
  | `tabBarMinimizeBehavior`, `tabViewBottomAccessory`, `onInteractiveResizeChange` | 26.0 |

### 1.10 Compose Multiplatform on iOS: window size is animated [V, JetBrains `WindowContext.ios.kt` on `jb-main`]
`prepareAndGetSizeTransitionAnimation` writes `_windowInfo.containerSize` and `containerDpSize` as a `lerp` **on every
animation frame** during a size transition, such as a rotation or a scene resize. Any composition that reads exact
`containerSize` therefore recomposes on every frame of an iPad rotation. On iOS, `containerSize` is the `UIWindow`
bounds.

### 1.11 Test tooling facts
- **`DeviceConfigurationOverride.WindowSize(DpSize)`** (compose ui-test 1.12.1) overrides `LocalWindowInfo` (both
  `containerSize` and `containerDpSize`) and `LocalConfiguration`. It reads the size through `rememberUpdatedState`, so a
  test can **resize live** by changing a `mutableStateOf(DpSize)` [V, source]. `ForcedSize` does not override
  `LocalWindowInfo`.
- **Compose Preview Screenshot Testing**, plugin `com.android.compose.screenshot` 0.0.1-alpha16 [V,
  https://developer.android.com/studio/preview/compose-screenshot-testing]:
  - Gradle tasks need AGP 8.5 or newer; the IDE integration needs AGP 9.0 or newer.
  - From **AGP 9.5.0-alpha03 the standalone plugin is deprecated** in favour of AGP test suites
    (`android.experimental.testSuiteSupport=true`, `testOptions.screenshotTests.create(...)`).
  - Rendering is host-side through layoutlib. There is no interaction and no runtime resize.
  - The known-issues list says it does not support non-Android targets in KMP projects.
- **Roborazzi** 1.76.0 (2026-09-29) [V, GitHub releases]:
  - The AGP 9 issue (#777) was fixed in 1.57.0, and KMP `androidHostTest` users confirmed it works (#781).
  - Generates tests from previews (`generateComposePreviewRobolectricTests`, which applies the preview's
    device, night mode and font scale; 1.75.0 made it 1.6× faster and added `renderScale`).
  - Experimental Compose Desktop preview tests (1.70.0), and an experimental UI-tree dump for AI agents (1.69.0).
- **Paparazzi** 2.0.0-alpha05.1 (2026-09-28) supports AGP 9.4.1, but issue #2491 is open: `SecureTextField` crashes
  at test SDK 37. The template ships `AppSecureField` [V, GitHub].
- Compose UI 1.12.1 lint has id **`ConfigurationScreenWidthHeight`**, message "Using
  Configuration.screenWidthDp/screenHeightDp instead of LocalWindowInfo.current.containerSize". Related ids:
  `LocalContextConfigurationRead`, `LocalContextResourcesRead` [V, `lint.jar` in the ui AAR].
- `@PreviewScreenSizes` (Phone, Phone landscape, Unfolded foldable, Tablet, Tablet landscape, Desktop) and
  **`@PreviewWrapper(PreviewWrapperProvider)`** are stable in ui-tooling-preview 1.12.1 [V, source]. The device
  strings are:

  | Constant | Spec |
  |---|---|
  | `Devices.PHONE` | `spec:width=411dp,height=891dp` |
  | `Devices.FOLDABLE` | `spec:width=673dp,height=841dp` |
  | `Devices.TABLET` | `spec:width=1280dp,height=800dp,dpi=240` |
  | `Devices.DESKTOP` | `spec:width=1920dp,height=1080dp,dpi=160` |

- Emulator console commands [V, https://developer.android.com/blog/posts/emulator-control-for-adaptive-app-development]:
  - `adb emu fold` and `adb emu unfold`.
  - `adb emu posture` (1 closed, 2 half-opened/tabletop, 3 opened).
  - `adb emu resize-display 0|1|2` (phone, unfolded, tablet).
  - `adb emu rotate`.

---

## 2. The adaptive layer, v2 (rank 1)

### 2.1 What is wrong today
1. **The whole tree recomposes on every size change, with no skipping.**
   - Where: `core/designsystem/.../theme/AppWindowSize.kt:28-57` and `:94-109`, and `AppTheme.kt:67-77` (WP04).
   - `AppWindowSize(widthDp, heightDp)` compares exact dp in `equals`. It is provided through
     **`staticCompositionLocalOf`**.
   - A changed static local recomposes "the entirety of the content" of the provider, and that content is the whole
     app.
   - Triggers: every configuration-delivered resize, fold and rotation on Android, and every frame of a size
     transition on CMP-iOS (§1.10).
2. **`StackHost` reads `LocalWindowInfo.current.containerSize.width` directly** (`core/navigation/.../StackHost.kt:101`)
   to compute `predictiveMillis`, so every host recomposes on any width change.
3. **Window class, not pane class.** A component inside a 480 dp detail pane on a 1280 dp window sees `Large`, and so
   lays out a tablet design in a phone-width pane. UIKit and SwiftUI handle this by overriding traits per split-view
   column [U, from memory].
4. **Posture and input traits are missing entirely.** There is no posture, no hinge, no pointer precision and no
   keyboard kind anywhere in the template [V, grep].
5. **Components barely read tokens.** Only `AppSheet`, `AppActionSheet` and `AppToast` read
   `AppTheme.layout.sheetMaxWidth`. `AppAlert` has a fixed 300 dp width, which is fine. `AppSection` and `AppRow`
   stretch the full window width on desktop [V, WP03 grep].

### 2.2 Design [D]
```kotlin
// :core:designsystem theme/AppWindowSize.kt (shared verbatim with CMP commonMain)
enum class WindowWidth { Compact, Medium, Expanded, Large, ExtraLarge }
enum class WindowHeight { Compact, Medium, Expanded }

/** Classes only: equality changes only when a breakpoint is crossed. */
@Immutable
data class SizeClass(val width: WindowWidth, val height: WindowHeight) {
    companion object {
        fun of(widthDp: Float, heightDp: Float): SizeClass { /* BREAKPOINTS_V2, as today */ }
    }
}

@Immutable
data class WindowPosture(
    val kind: Kind = Kind.Flat,   // Flat | Tabletop | Book
    val hinge: Rect? = null,      // separating hinge, window px; null when not separating
) { enum class Kind { Flat, Tabletop, Book } }

@Immutable
data class InputTraits(
    val finePointer: Boolean = false,      // mouse, trackpad or stylus attached
    val hardwareKeyboard: Boolean = false,
    val touchscreen: Boolean = true,
)

/** Exact size, for the few things that need it. Reading it subscribes only the reader. */
@Stable
class WindowMetrics internal constructor(private val info: WindowInfo, private val density: Density) {
    val sizeDp: DpSize get() = with(density) { info.containerSize.toSize().toDpSize() }
}
```
- `AppTheme` (and nothing else) does the following:

  ```kotlin
  val info = LocalWindowInfo.current; val density = LocalDensity.current
  val windowClass by remember(info, density) { derivedStateOf { info.containerSize.toSizeClass(density) } }
  ```

  The derived state reads `containerSize`, but **`AppTheme` recomposes only when the class changes**.
- It provides these locals:
  - `LocalWindowClass`: static. It changes only at breakpoints, and a breakpoint means a real relayout anyway.
  - `LocalContainerClass`: static, initially equal to `LocalWindowClass`.
  - `LocalWindowMetrics`: static; the object's identity is stable and its reads are snapshot reads.
  - `LocalWindowPosture` and `LocalInputTraits`: static. These come from the Activity (§5, §2.4) as plain parameters,
    so the design system stays free of Activity code and works on CMP.
- `AppLayout.forClass(containerClass)` is computed from the class only, as today but keyed on the class.
- `AppTheme.windowClass` drives the shell: bar, rail or sidebar, and the pane count.
- **`AppTheme.containerClass`** drives components and screens. Each pane in a scene re-provides it from the pane's own
  width:

  ```kotlin
  @Composable fun ProvideContainerWidth(widthDp: Float, content: @Composable () -> Unit) {
      val parent = LocalContainerClass.current
      val cls = remember(widthDp, parent) { parent.copy(width = WindowWidth.of(widthDp)) }  // height unchanged
      CompositionLocalProvider(LocalContainerClass provides cls, LocalAppLayout provides AppLayout.forClass(cls), content = content)
  }
  ```

  The scene knows pane widths at composition time, because they are tokens (list 360/412 dp). The detail pane width is
  `window − chrome − list − divider`, quantised to a class, so it changes only at breakpoints.
- `StackHost`'s `predictiveMillis` reads `LocalWindowMetrics.current.sizeDp` **inside** the transition-spec lambda,
  which runs at transition time, not during composition.
- Gate (ties to requirement 22): a resize *inside* a class (900 → 1000 → 1100 dp) causes **0 recompositions** of
  screen roots and at most 1 of the shell chrome. This uses the recomposition-count harness from `RECOMPOSITION-PLAN`
  (§6.4 test C).

Expected effect: a class change recomposes once, which is unavoidable because the layout changes. Other size changes
cost 0 recompositions in the design system's consumers. Android still recomposes callers of `stringResource`, because
`LocalResources` reads `LocalConfiguration` [V, `AndroidCompositionLocals.android.kt:46-57`]; that is cheap compared
with recreating the activity. On CMP-iOS, a rotation goes from N full-tree recompositions to at most 1 [U, measure
on the iOS CI job if possible].

### 2.3 Tokens per class [D, numbers aligned with Material §1.1 and Apple readable widths]

| Token | Compact | Medium | Expanded | Large | Extra-large |
|---|---|---|---|---|---|
| `gutter` | 16 | 24 | 24 | 32 | 32 |
| Navigation chrome (window class) | floating bar | rail | rail (sidebar opt-in) | sidebar | sidebar |
| Horizontal panes (window class) | 1 | 1 | 2 | 2 (3 with an extra pane) | 3 |
| `listPaneWidth` | — | — | 360 | 412 | 412 |
| `supportingPaneWidth` | sheet | sheet | 360 | 412 | 412 |
| `readableMaxWidth` (prose, settings, forms 480) | full | 720 | 720 | 720 | 720 |
| `contentMaxWidth` (mixed) | full | full | 1040 | 1040 | 1200 |
| `gridMinCellWidth` (feeds) | 160 for tiles, 300 for cards (two tokens) | same | same | same | same |
| Sheet presentation | bottom with detents | form sheet when height ≥ Medium | form sheet | form sheet | form sheet |

Compact height (a phone held sideways, 891×411):
- The rail replaces the bar; the template does this already.
- Large titles render inline.
- A sheet opens at full height, capped at `sheetMaxWidth` and centred.
- `AppTwoPane` puts its halves side by side (`shouldUseTwoPanes`, which already exists).

### 2.4 Input traits source [D]
Android (`:app`, `MainActivity`), one per window:
- `InputManager.registerInputDeviceListener`, which recomputes `finePointer` (`SOURCE_MOUSE`, `SOURCE_TOUCHPAD`,
  `SOURCE_STYLUS` with a valid X motion range: the same rule as Compose's own `resolvePointerPrecision` [V]),
  `hardwareKeyboard` (`KEYBOARD_TYPE_ALPHABETIC && !isVirtual`) and `touchscreen`.
- These are passed into `AppTheme(inputTraits = …)`.
- Popups inherit them through composition locals, so the cost is one listener per Activity, not one per popup (unlike
  `mediaQuery`, §1.4).
- When window 1.6 is stable, `windowEngagementInfo(PRECISE_POINTER)` can replace the device scan.

---

## 3. Navigation chrome: bar → rail → sidebar (rank 3)

### 3.1 Today
- One `TabbedNavHost` call site, so the width class changes only the chrome. The tabs, stacks and ViewModels survive
  folds, rotations and resizes (`AppShell.kt:121-170`; VitFleet B10 ported) [V, code]. Keep this.
- Defects in the rail (`component/navigation/AppTabBar.kt:480-525`, WP04) [V, code]:
  - Items are a plain `Column` of fixed `height(RailItemHeight = 68.dp)` with no scroll.
    - 5 tabs × 68 dp plus 2 × `spacing.lg` is about 388 dp, which only just fits a phone held sideways (411 dp minus
      the status bar).
    - **6 or more tabs clip.**
    - The labels clip at font scale 2.
  - `trailingAction` (for example search) and `accessory` are passed as `null` in rail mode
    (`AppShell.kt:146`), so search and a mini-player **disappear** on wide windows.
  - There is no sidebar, and no cap or "More" rule for tab counts (requirement 18 asks for a cap of 5 plus More on
    the bar).

### 3.2 Target [D, parity: M3 rail (collapsed 96 dp, expanded 220-360 dp, which replaces the drawer; [S] 9to5google) and iPadOS 18 `sidebarAdaptable`]

| Window | Chrome | Items | Search / primary action | Accessory |
|---|---|---|---|---|
| Compact width | floating bar (existing) | ≤ 5, else 4 + "More" | trailing capsule (existing) | above the bar (existing) |
| Medium, or Compact height | rail, 88 dp | ≤ 7, else 6 + More; labels hidden in Compact height; `verticalScroll` fallback | a rail header button at the top | rail footer |
| Expanded | rail; a sidebar toggle in the rail header | as rail | header | footer |
| Large and Extra-large | **sidebar**, 280 dp (304 at Extra-large); collapsible to the rail | all tabs, grouped by `ShellTab.section` (like `TabSection`); `heightIn(min = 48.dp)` rows; selected row tinted | search field at the top of the sidebar (iPadOS) | bottom of the sidebar |

- **Sidebar collapse and expand: lay out once, animate transforms.**
  - Apply the content's final width in the first frame (one relayout).
  - Animate the sidebar's clip and translation and the content's `translationX`, from the old start offset to 0, in
    `graphicsLayer` only, on `springs.layout`.
  - No per-frame relayout and no per-frame recomposition.
  - Gate: `sidebar_toggle` janky frames ≤ 0.5 % at 120 Hz (§8).
- **A class flip (bar ↔ rail ↔ sidebar) is not animated.** Android and iPadOS both relayout instantly. Animating it
  would mean relaying out every frame.
- The collapsed state is per user: a saveable in the shell, persisted to settings when the app has a settings store.
  Keyboard: Ctrl+Shift+S on Android, ⌃⌘S on iOS, the macOS convention [D].
- The selection indicator stays draw-phase only (the existing rule) in all three forms. The sidebar's selected-row
  highlight uses the same `springs.selection`.
- The rail and sidebar must clear the cutout and the navigation bar on the start side (they do today, with
  `safeDrawing.only(Start)`). With 3-button navigation held sideways, check that content clears the **end**-side
  navigation bar [U, verify on the Redmi in landscape].
- The predictive-back card (requirement 24) shrinks inside the content area. The rail and sidebar stay still; the host
  is already `clipToBounds()` in rail mode.

---

## 4. Panes: list-detail, supporting, extra, hinge (rank 4)

Today: `ListDetailScene.kt` (WP04).
- Strengths:
  - List and Detail metadata, and the newest detail on top.
  - The scene keyed by the list, so changing the selected item swaps in place.
  - `equals` implemented.
  - `sameStack()` means a fold, unfold or resize redraws without a slide (`NavTransitions.kt:116-123`, with
    `SceneChangeTest`) [V, code].
- Gaps, all [D] proposals:

1. **More pane kinds.** `enum class NavPane { Single, List, Detail, Supporting, Extra }`:
   - `Supporting`: shown beside its main entry from Expanded at `supportingPaneWidth`. Below Expanded it is presented
     as a sheet (medium detent) through the presentation host. This is iOS `.inspector` behaviour and Material's
     supporting pane reflow.
   - `Extra`: a third column (list | detail | extra) from **Large**, the same as Material's `extraPane` and its three
     partitions. Below Large it is pushed over the detail.
   - Switch the metadata to Nav3 1.2's `NavMetadataKey<NavPane>` (typed).
2. **Panes are scoped classes.** Wrap each pane in `ProvideContainerWidth` (§2.2), so a screen inside the detail pane
   adapts to the pane's width.
3. **Pane context for screens.**
   - `LocalPaneContext` provides `role`, `twoPane`, `detailDepth` and `selectedKey`.
   - `AppTopBar` hides the Back arrow when `role == Detail && twoPane && detailDepth == 1`. The Google skill makes this
     a rule.
   - The list pane reads `selectedKey`, so the open row shows `selected` (iPad Mail and Settings behaviour).
   - Full-screen detail chrome (`FullScreenKey`) is turned off inside a two-pane scene. This is also a skill rule.
4. **Back in two panes.**
   - Back pops the detail stack, then closes the detail and shows the placeholder, then defers to the shell.
   - The predictive back uses the Gmail card constants **applied to the detail pane's bounds**: the pane shrinks and
     the list stays still.
   - Today the detail swaps instantly inside the scene (`key(detail?.contentKey)`), so a back gesture between details
     gives no feedback.
5. **Hinge-aware split.**
   - When `WindowPosture.kind == Book` and the hinge is separating, the list takes the space before the hinge and the
     detail the space after it. Nothing is drawn under an occluding hinge (Material: `HingePolicy.AvoidSeparating`
     [V]).
   - In `Tabletop`, a scene with a list and a detail may stack them top and bottom of the fold (Material allows two
     vertical partitions in tabletop [V]).
   - The hinge rect is in window pixels. Capture the scene's window offset with `onPlaced` into a state read in
     measure. That costs one relayout on a fold change, which is rare.
6. **Pane resizing (desktop `UI_Config`).** Optional at Expanded and wider:
   - A 1 dp divider with a 48 dp-wide invisible drag target.
   - `PointerIcon.Crosshair` or a resize cursor on hover [U: Compose has no horizontal-resize `PointerIcon` constant;
     check `PointerIcon` in ui 1.12.1].
   - Keyboard: arrow keys when the divider is focused.
   - The list width is persisted per window. Snap anchors at 320, 360 and 412 dp.
7. **State continuity across scene changes.**
   - The list entry moves between `ListDetailScene` and the single-pane scene, so its composition is recreated.
   - What survives: `rememberSaveable` (the saveable decorator) and ViewModels (the ViewModel-store decorator).
   - What is lost: plain `remember`.
   - Rule for screens: scroll state, text and expansion use saveable state (`rememberLazyListState` is saveable).
     Test: §6.4 test B.
   - Nav3 1.2's `SharedEntryInSceneNavEntryDecorator` might keep the entry instead [U: what it does exactly is
     unverified; spike before relying on it].

Feed grid: `LazyVerticalGrid(GridCells.Adaptive(AppTheme.layout.gridMinCellWidth))`, which is the WP03 replacement
for `AppResponsiveGrid`.
- Headers use `span = { GridItemSpan(maxLineSpan) }`.
- `contentPadding` comes from the gutter.
- In a two-pane list, the grid automatically gets fewer columns, because it reads the pane's constraints.

---

## 5. Configuration, multi-window, multi-instance (rank 2)

1. **`configChanges` for size, keyboard and navigation** (`app/src/main/AndroidManifest.xml:50-61` has none today):
   ```xml
   <activity android:name=".MainActivity"
       android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden|navigation"
       … >
   ```
   - The first four come from Google's recommendation [V].
   - `keyboard|keyboardHidden|navigation` covers Tier 3 `Keyboard_Input`: attaching a keyboard must not relaunch the
     activity [V, guideline; that these flags are exactly what changes on attach is U, verify with `adb shell dumpsys
     activity` while toggling a keyboard on the emulator].
   - Leave `density`, `uiMode`, `locale` and `fontScale` to recreation, because resources must reload. Whether
     fold/unfold on a Pixel Fold changes density, which would still recreate, is [U, check with `adb emu fold`].
   - Effect: resize, fold, rotation and keyboard attach no longer recreate the activity, rebuild every composition or
     re-run startup work. KeptTabs keep even their `remember` state.
   - Hard rule: any `AndroidView` or `AndroidFragment` must handle `onConfigurationChanged` itself [V, docs].
   - The `restore` perf scenario (process death) stays mandatory, because recreation still happens.
2. **Multi-instance safety (a defect today).**
   - `AppNavigator` is `@Singleton` with one `Channel`, consumed by `applyWhileStarted`. Its own KDoc says "the
     navigator is one per process" (`core/navigation/.../AppNavigator.kt:40-80`).
   - With two windows, which are both STARTED and RESUMED under multi-resume, each command reaches whichever
     collector receives it. A tap in window A can navigate window B.
   - `PendingDeepLink` (`DeepLinks.kt:61`) is also `@Singleton`.
   - Fix: **`@ActivityRetainedScoped`** for both. Hilt's `ViewModelComponent` is a child of
     `ActivityRetainedComponent`, so ViewModels still inject the navigator, and it survives configuration changes [V,
     https://dagger.dev/hilt/components].
   - Then declare `PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI=true` (§1.8). Keep `launchMode="singleTop"`; it is
     compatible with `FLAG_ACTIVITY_MULTIPLE_TASK`.
   - Test: two `ActivityScenario`s in one process; a navigation command from A's ViewModel changes only A's stack.
3. **Default window size on desktop**: `<layout android:defaultWidth="1024dp" android:defaultHeight="720dp"
   android:minWidth="320dp" android:minHeight="400dp"/>` on `MainActivity` [U: how Googlebook and Android desktop
   windowing honour `<layout>` defaults needs the desktop emulator. `minWidth` 320 dp is our smallest tested size,
   §6.3].
4. **Header bar** (optional, Tier 1): leave the system caption bar as it is by default.
   - `safeDrawing` already includes `captionBar`, through `systemBars` [U, verify on the desktop emulator].
   - Drawing the app's top bar under a transparent caption bar is a later step, because it needs the bounding rects.

---

## 6. Enforcement (rank 5): how the template forces adaptivity

### 6.1 Build-time rules
1. **Lint errors** (`AndroidCommon.kt`, extending `lint.error`): `ConfigurationScreenWidthHeight`,
   `LocalContextConfigurationRead`, `LocalContextResourcesRead` [V, ids exist in ui 1.12.1]. Keep the three
   orientation and resizability ids.
2. **`verifyAdaptiveRules`**, a Gradle task in `build-logic`. It follows the same pattern as
   `verifyModuleDependencies` and `checkComposeStability`: a cacheable task over the `src/main` Kotlin sources of
   `:feature:*`, `:catalog` and `:core:ui`. It is a text scan, not lint, so it has no lint-API version coupling with AGP.
   Each hit fails with the file, line and fix. An allowance is `// adaptive: allow <reason>` on the line, and an
   allowance without a reason fails.

   | Rule | Pattern (regex, simplified) | Fix it points to |
   |---|---|---|
   | A1 orientation reads | `configuration\.orientation\|ORIENTATION_(LANDSCAPE\|PORTRAIT)\|screenOrientation` | `AppTheme.windowClass` |
   | A2 screen metrics | `screen(Width\|Height)Dp\|displayMetrics\.(width\|height)Pixels\|defaultDisplay\|getRealMetrics\|currentWindowMetrics\|LocalWindowInfo` (outside `:core:designsystem`) | `AppTheme.containerClass`, constraints |
   | A3 device-type checks | `isTablet\|userInterfaceIdiom\|smallestScreenWidthDp\|Build\.MODEL\|R\.bool\.is_tablet` | classes |
   | A4 screen-sized fixed dimensions | `\.(width\|requiredWidth\|size)\(\s*([3-9]\d\d\|\d{4,})\.dp` and `\.(height\|requiredHeight)\(\s*([5-9]\d\d\|\d{4,})\.dp` | `widthIn(max = AppTheme.layout.*)`, weights, `fillMax*` |
   | A5 fixed fractions of the screen | `fillMax(Width\|Height)\(0?\.\d+f?\)` at screen level | pane tokens |
   | A6 orientation locks | `setRequestedOrientation\|PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY\|resizeableActivity="false"\|(min\|max)AspectRatio` (Kotlin and merged manifest) | none; these are forbidden |
   | A7 `BoxWithConstraints` in a screen | `BoxWithConstraints` in `:feature:*` | `containerClass` or a custom `Layout` (SubcomposeLayout costs a frame) |
   | A8 screen previews | every `fun \w+Screen\(` in `:feature:*` has a matching `@AppScreenPreviews` function in the module | the annotation (§6.2) |
   | A9 global `mediaQuery` flag | `isMediaQueryIntegrationEnabled` | `AppTheme.inputTraits` and `windowPosture` |
   | A10 Material adaptive | `material3.adaptive\|NavigationSuiteScaffold` in any build file | `NavPane`, `AppShell` |

3. **Manifest check** on the merged manifest:
   - `MainActivity` has the `configChanges` set from §5.
   - The multi-instance property is present.
   - None of the A6 attributes appear.
4. **Generator side** (WP01/WP09 matrix): `generator/tests` asserts that every preset (lean to full) emits these
   checks, so nothing switches them off.

### 6.2 Multi-size previews
`:core:designsystem` ships, in a `preview/` package with `ui-tooling-preview` as an `implementation` dependency (it is
small):

```kotlin
@Preview(name = "Phone", device = Devices.PHONE)
@Preview(name = "Phone landscape", device = "spec:width=891dp,height=411dp")
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Desktop", device = Devices.DESKTOP)
@Preview(name = "Phone, font 2x", device = Devices.PHONE, fontScale = 2f)
@Preview(name = "Phone, dark", device = Devices.PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@PreviewWrapper(AppPreviewTheme::class)   // wraps in AppTheme(fonts = PlatformFonts); stable in 1.12.1 [V]
annotation class AppScreenPreviews
```

- `AppPreviewTheme` is a `PreviewWrapperProvider`, so no preview forgets the theme.
- Screens preview their *stateless* `XScreen(state, onEvent)`. That already matches the MVI rule ("stateless UI +
  @Preview").
- Rule A8 makes the previews mandatory.

### 6.3 Screenshot tests: choose Roborazzi [D]
Why Roborazzi rather than Google's Compose Preview Screenshot Testing (CPST) or Paparazzi:
- **It reuses the infrastructure already here.** The template already runs Robolectric 4.17 Compose tests with
  `@GraphicsMode(NATIVE)` and `captureToImage()` (WP03 handoff, `RowHighlightTest`) [V]. Roborazzi is built on exactly
  that.
- **It can test behaviour, not just render previews.** It handles resizing live (`DeviceConfigurationOverride.WindowSize`),
  clicks, keyboard and mouse input (`performMouseInput { rightClick() }`), and opened sheets and menus. CPST renders
  previews only.
- **CMP.** Roborazzi supports KMP `androidHostTest` under AGP 9 [V, #781] and has experimental desktop preview tests.
  CPST lists non-Android KMP targets as unsupported [V].
- **Stability.** CPST's standalone plugin is deprecated from AGP 9.5.0-alpha03 in favour of experimental test suites
  [V]. Paparazzi is alpha, with the open SDK 37 `SecureTextField` crash [V].
- **Exit path.** `@AppScreenPreviews` previews are tool-agnostic, so moving to CPST test suites later is mechanical.
  Keep previews free of Roborazzi APIs.

Setup (a convention plugin `<pkg>.android.screenshots`, applied to `:core:designsystem`, `:catalog`, `:feature:*`):

```kotlin
plugins { id("io.github.takahirom.roborazzi") }            // 1.76.0 [V, release]
roborazzi {
    @OptIn(ExperimentalRoborazziApi::class)
    generateComposePreviewRobolectricTests {
        enable = true
        packages = listOf("<pkg>")
        robolectricConfig = mapOf("sdk" to "[35]")            // the template's TEST_SDK
        // renderScale = 0.5  // experimental in 1.75.0: smaller goldens, faster [V, release notes]
    }
}
// testImplementation: roborazzi, roborazzi-compose, roborazzi-compose-preview-scanner-support,
//   ComposablePreviewScanner (version as pinned by the Roborazzi release) [U: exact coordinates/versions]
// unit tests: systemProperty("robolectric.graphicsMode", "NATIVE")
```

- Tasks: `recordRoborazziDevDebug`, `verifyRoborazziDevDebug` and `compareRoborazziDevDebug`, named per flavour.
- **Size matrix.** The PR matrix covers every width class and every height class at least once:

  | Id | dp | Classes | Represents |
  |---|---|---|---|
  | phone | 411×891 | C×M | phone (`Devices.PHONE`) |
  | phone-land | 891×411 | E×C | phone held sideways |
  | fold | 673×841 | M×M | unfolded foldable (`Devices.FOLDABLE`) |
  | tablet-8 | 1024×640 | E×M | 8" tablet in landscape (Google test device) |
  | tablet-10 | 1280×800 | L×M | 10.5" tablet in landscape (`Devices.TABLET`) |
  | desktop | 1920×1080 | XL×E | desktop, Googlebook (`Devices.DESKTOP`) |

  The nightly run adds: 320×568 (the smallest supported window), 800×1280 (M×E), font scale 2 at phone and
  tablet-10, dark at phone, and RTL at tablet-10.
- **Goldens in generated projects.** The user's colours and fonts differ, so the template's goldens do not apply.
  - The generator ships the tests without goldens.
  - The README and AGENTS tell the developer to run `recordRoborazziDevDebug` once.
  - CI runs `verify…` only when goldens exist, and otherwise `compare…` with a warning.
  - The *golden-free audit* below is what enforces adaptivity in generated projects from day one.

### 6.4 Golden-free tests that always run
1. **Adaptive audit** (`AdaptiveAuditTest`).
   - For each registered destination with preview state: the `NavRegistry` exposes a sample-state hook. Rule A8
     already guarantees previews, so the audit can reuse the preview functions through ComposablePreviewScanner.
   - Each one renders at the six PR sizes and at font scale 1 and 2.
   - It asserts:
     - **No clipped text:** `GetTextLayoutResult` semantics, `hasVisualOverflow == false` unless the node opted into
       ellipsis with `maxLines`.
     - **No unreachable controls:** every clickable node's bounds lie inside the window and are not covered by
       `LocalBottomChrome`.
     - **Touch targets ≥ 48 dp** (Tier 2).
     - **No overlapping clickables.**
     - **No stretched text:** no text node wider than `readableMaxWidth + gutter` at Large and wider. This is what
       Android Studio's UI Check flags as "text stretched on large screens".
   - Also run `enableAccessibilityChecks()` from `ui-test-junit4-accessibility` [V that the artifact exists; U whether
     ATF checks behave under Robolectric].
2. **Resize continuity** (Robolectric with compose ui-test; uses
   `DeviceConfigurationOverride.WindowSize(sizeState)`):
   - **A. Tabs survive class flips.**
     - Set up the `AppShell` with three fake tabs. Each tab root holds a `remember { Any() }` identity, a
       `rememberSaveable` counter and a ViewModel.
     - Push a detail on tab 2, then walk the sizes 411 → 891 → 1280 → 1920 → 673 → 411.
     - Assert:
       - No identity changed: no tab was disposed.
       - The stacks and selected tab are unchanged.
       - The chrome tag is `tab_bar`, `rail` or `sidebar` as expected.
       - `BackGate.moving` stayed false: no transition ran.
   - **B. List-detail flip.**
     - Open the list and a detail at 1280 (two panes), scroll the list to item 30, and resize to 411.
     - The detail is now on top, single pane. Press Back: the list shows at item 30.
     - Resize back: two panes, the detail selected and its row marked selected.
   - **C. No work inside a class.** Resize 900 → 1000 → 1100: recompositions of screen roots == 0, and of the chrome
     ≤ 1. This uses the requirement 22 harness.
   - **D. Posture.** Provide `WindowPosture(Tabletop, hinge)`: the image viewer splits at the hinge, and the sheet's
     medium detent equals the fold line. Provide `Book`: list-detail splits at the hinge.
   - **E. Input.**
     - `performKeyInput { pressKey(Key.Tab) }` traverses the focus order at each size.
     - Esc closes the top sheet.
     - Ctrl+F focuses search.
     - `performMouseInput { rightClick() }` on a row opens its context menu and does **not** call `onClick` (§7.1
       bug).
     - Hovering a row shows the hover highlight.
   - **F. Multi-instance.** Two activities in one process: a command from A's ViewModel changes only A.
   - **G. No recreation.** In Robolectric, `RuntimeEnvironment.setQualifiers("+land")` and then `w1280dp-h800dp`:
     `onCreate` count == 1 (because of `configChanges`). The `restore` scenario still covers process death.
3. **Device scripts** (`tools/adaptive/adaptive.sh`, run through `$O/bin/locked adb`):
   - On the emulator (a Resizable or Pixel Fold AVD; the user's `periflow_api35` is a phone AVD):
     - Run `adb emu resize-display 0/1/2`, `adb emu fold`/`unfold`, `adb emu posture 2/3` and `adb emu rotate`.
     - After each step, assert from the `perf.shell` log and the UI dump (`android layout --flat`, or
       `uiautomator dump`) that:
       - the selected tab and the stacks are unchanged;
       - the chrome type matches the class;
       - no crash occurred.
   - Cost: a second AVD uses about 2-3 GB of RAM on a 15 GB machine. Run it serially through the emulator lock
     [U, measure].

### 6.5 Docs rules for agents (with WP12)
1. **AGENTS.md** gets a short, enforceable block:
   ```
   ## Adaptive UI (enforced by verifyAdaptiveRules, lint, AdaptiveAuditTest)
   - Decide layout from AppTheme.containerClass (components, screens) or AppTheme.windowClass (shell only).
     Never from orientation, device type, Configuration.screenWidthDp or LocalWindowInfo.
   - Panes: declare entry(pane = NavPane.List/Detail/Supporting/Extra). Never build your own two-pane Row for
     navigation; the shell decides when panes sit side by side.
   - No fixed screen-sized dimensions (≥320.dp wide, ≥480.dp tall). Use widthIn(max = AppTheme.layout.*), weights.
   - Every XScreen has an @AppScreenPreviews preview of its stateless UI.
   - Keep state that must survive fold/resize in rememberSaveable or the ViewModel; plain remember is lost when a
     pane moves between scenes.
   - Pointer and keyboard: clickable things use Modifier.pressable (hover, focus ring, right-click → context menu
     included); give screens their shortcuts through AppShortcuts.
   - Never re-record screenshot goldens to make a failing test pass; show the diff to the user.
   - The vendored `adaptive` skill is Material-based. In this project use AppShell (not NavigationSuiteScaffold),
     NavPane scenes (not material3 adaptive-navigation3), LazyVerticalGrid (not experimental Grid on Compose 1.12),
     AppTheme traits (not the global mediaQuery flag), and Roborazzi (not com.android.compose.screenshot).
   ```
2. **`docs/ADAPTIVE.md`** (the full guide):
   - The class table (§2.3), the chrome table (§3.2) and the pane rules (§4).
   - The input checklist (§7), the tier mapping (§1.7), the test commands (§6.3, §6.4) and the device scripts.
   - Root causes we found: the static-local window size, the singleton navigator, the rail overflow and the
     right-click bug.
   - The measured numbers from §8 once they exist.
3. **Pre-merge checklist** (requirement 25) gains these lines:
   - "`./gradlew verifyAdaptiveRules lint` green."
   - "`AdaptiveAuditTest` and `ResizeContinuityTest` green."
   - "Screens checked at phone, phone-land, fold, tablet-10, desktop (Roborazzi compare report)."
   - "Keyboard: Tab order, Esc, shortcuts; mouse: hover, right-click."
4. **The skill override note** also goes into `.claude/skills/adaptive/` as a `PROJECT-NOTES.md` next to the vendored
   `SKILL.md` (requirement 26). A model that loads the skill then sees the project rules in the same folder [D].

---

## 7. Input parity (rank 7)

### 7.1 Defect: right-click fires `onClick`
- `Modifier.pressable` (WP03 `foundation/Pressable.kt:293-296`) uses `detectTapGestures`, which waits with
  `awaitFirstDown()`.
- On Android, `firstDownRefersToPrimaryMouseButtonOnly() = false`, so **a secondary or middle mouse button press counts
  as a tap** [V, foundation 1.12.1 `TapGestureDetector.kt:131,336-350` and `TapGestureDetector.android.kt:25`].
- A right-click on a row on ChromeOS or Googlebook therefore opens the row instead of a menu.
- Fix: in `PressableNode`, when every change is `PointerType.Mouse` and `!buttons.isPrimaryPressed`, route a secondary
  press to `onSecondaryClick`. Foundation's own right-click detector uses the same rule,
  `event.buttons.isSecondaryPressed && changes.fastAll { it.changedToDown() }` (`RightClickGestures.kt:43-49`), but it
  is `internal`, so re-implement the 10 lines.
- Add `onSecondaryClick: ((Offset) -> Unit)?` to `pressable`, and wire `AppRow` and `AppCard` to open the same
  `AppMenu`/context menu as long-press, anchored at the pointer. Skip the lift-and-preview animation for mouse: the
  desktop convention is an instant menu.

### 7.2 The input checklist and how to implement it [V for API existence in foundation and ui 1.12.1 sources]

| Item (tier id) | Implementation |
|---|---|
| Hover (`Hover_States`, `Hover_Parity`) | `PressableNode` delegates a hover node and emits `HoverInteraction`. Draw a subtle highlight (the `RowHighlight` colour at about 50 %) only while hovered, in the draw phase. No hover on touch. |
| Pointer icons (`Custom_Cursors`) | `Modifier.pointerHoverIcon(PointerIcon.Hand)` from `pressable` for links and buttons. Text fields already give the I-beam. `stylusHoverIcon` exists too. |
| Focus (`Drawable_Focus`, `Keyboard_Navigation`) | `pressable` already has a focus ring and Enter, Space and DPad-centre activation [V, code]. Add `Modifier.focusRestorer()` on lazy lists and the rail or sidebar, so Tab returns to the last item. Give the bar, rail and sidebar `focusGroup()`. Make the visual and focus order match in two-pane layouts: list then detail. |
| Shortcuts (`Keyboard_Shortcuts`, `Keyboard_Parity`) | `AppShortcuts`, a small registry of shell-level `onPreviewKeyEvent` handlers. Defaults: Ctrl+F search, Ctrl+N "new" where a screen registers one, Ctrl+1…9 select tab, Ctrl+[ / Alt+← back, Ctrl+Shift+S sidebar. List them in the system Keyboard Shortcuts Helper by overriding `Activity.onProvideKeyboardShortcuts(data, menu, deviceId)` with `KeyboardShortcutGroup` (Meta+/, API 24+) [V, https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/keyboard-shortcuts-helper]. Text editing shortcuts (copy, paste, undo) come from `BasicTextField`. |
| Esc (`Keyboard_Exit`) | Physical Escape falls back to Back in the platform key map [U: verify the `Generic.kcm` fallback on the emulator], so BackGate and the sheets handle it. Add an explicit `Key.Escape` handler on sheets and menus in case the fallback is consumed. |
| Enter to send (`Keyboard_Send`) | `AppTextField` takes `onSubmit`. Enter submits when `hardwareKeyboard` is true; Shift+Enter inserts a newline. |
| Scroll indicators (`Scrollbar_Display`) | `ScrollableState.scrollIndicatorState` (`scrollOffset`, `contentSize`, `viewportSize`) exists in foundation 1.12.1 for `ScrollState` and the lazy states [V]. Build an iOS-style indicator: a thin capsule that fades in while scrolling, drawn in a `DrawModifierNode` reading the state in draw only. With a fine pointer it is wider and draggable. |
| Zoom (`Content_Zoom`) | `AppImageViewer`: Ctrl+wheel (a `PointerEventType.Scroll` with `isCtrlPressed`) and trackpad pinch through the existing transform gestures. |
| Mouse wheel and trackpad scroll | Works by default in Compose. With the readable-width rule (§2.3), keep lazy lists **full window width** and centre the items (`horizontalAlignment = CenterHorizontally`, items `widthIn(max = readable)`), so the wheel scrolls anywhere and the indicator sits at the window edge. |
| Multi-select (`Input_Combinations`) | `AppReorderableList` and `AppSelection` take Shift-range and Ctrl-toggle when the pointer is fine (`PointerKeyboardModifiers`). This is in scope only for components that already support selection. |
| Drag and drop (`Drag_Drop_Support`) | Recipe in `docs/ADAPTIVE.md`, plus catalog demo: `dragAndDropSource` / `dragAndDropTarget` for images and text, and opening an item in a new window with the flags from §1.8. Not wired into every component. |
| Stylus (`Stylus_Text_Input`) | `BasicTextField` already calls `stylusHandwriting(...)` [V, `BasicTextField.kt:400`], so `AppTextField` gets handwriting on Android 14+. `AppSearchField` used as a button needs `Modifier.handwritingDetector { focusRealField() }` [V, the API exists]. |
| Targets | Keep `minimumTouchTarget` at 48 dp [V, `Pressable.kt:510`]. A denser `InputTraits` (fine pointer, no touchscreen) may tighten row padding, never below 48 dp, because Tier 2 requires it. |

---

## 8. Perf: adaptive scenarios on the reference phone (ties to requirements 19, 20, 22)

A tablet-class window can be measured **on the Redmi** (1080×2400 px) by changing its logical density. It keeps the
reference GPU and the 120 Hz budget [D; computed, U until run]:
- `adb shell wm density 260`: portrait is 664×1477 dp (M×E). Held sideways (`settings put system
  accelerometer_rotation 0; settings put system user_rotation 1`) it is 1477×664 dp (**L×M**): the rail or sidebar
  plus two panes.
- `wm density 240`: sideways it is 1600×720 dp (**XL×M**).
- Restore with `wm density reset` and `user_rotation 0`. The perf script must restore these on exit, using a trap.

New `perf.sh` scenarios (gates in `gates.toml`, the same columns as the phone rows):

| Scenario | Gate (solid chrome) |
|---|---|
| `tabs_large` (sidebar or rail, density 260 sideways) | janky ≤ 1.0 %; first frame of a visited tab ≤ 8 / ≤ 16 ms (same as phone) |
| `listdetail_select` (tap 20 rows; the detail swaps in place) | janky ≤ 1.0 %; first frame of the detail swap ≤ 12 / ≤ 20 ms (the push target) |
| `pushpop_large`, `scroll_large` | same as the phone rows |
| `sidebar_toggle` (10 toggles) | janky ≤ 0.5 %, 0 recompositions per frame |
| `resize` (functional, emulator: resize-display 0→1→2→0, fold/unfold) | no crash, stacks and tab unchanged, no activity recreation (logcat `onCreate` count) |
| PSS after the large journey | ≤ phone PSS + 15 MB (more content on screen) [D] |

Nothing here has been run yet. The final report (requirement 20) should add the large-layout rows for old and new.

---

## 9. Compose Multiplatform parity (rank 10, WP07)

1. **`AppWindowSize.kt` and `AppLayout` in `commonMain`, unchanged.**
   - `window-core` 1.5.1 has iOS artifacts [V]. Add them to `sync.toml` as an identical pair.
   - The class gating of §2.2 is **mandatory** here, because iOS lerps `containerSize` every frame (§1.10).
   - Source: `LocalWindowInfo.current.containerSize` with density (iOS sets both values [V]).
2. **Traits as `expect`/`actual`.**
   - `WindowPosture`: Android uses `WindowInfoTracker`; iOS is always `Flat`.
   - `InputTraits`: Android uses `InputManager`. iOS uses `GCKeyboard.coalesced != nil` for a hardware keyboard and
     assumes a possible fine pointer on iPad, because hover only renders on hover anyway [U: the GameController
     approach is unverified; check iPad trackpad detection on the CI device].
3. **iOS input exists in CMP.** `jb-main` has iOS `PointerButtons`, `PointerIcon`, key events and
   `IosDragAndDropManager` [V, file presence; behaviour U].
   - Info.plist: `UIApplicationSupportsIndirectInputEvents = YES` so trackpad and mouse arrive as pointer events. The
     JetBrains `codeviewer` example sets it [V, file].
   - The §7.1 right-click fix lives in shared code. Check `firstDownRefersToPrimaryMouseButtonOnly` on iOS [U].
4. **iPad Info.plist (XcodeGen `info.properties`):**
   - All four `UISupportedInterfaceOrientations~ipad`.
   - No `UIRequiresFullScreen`.
   - `UILaunchScreen`, which iOS 27 requires [V].
   - `UIApplicationSupportsMultipleScenes` false for v1: Compose windows are one scene per `ComposeUIViewController`,
     and multi-window is not in v1 [D].
   - The A12 static checks assert all of this.
5. **Chrome policy per platform** [D].
   - Shared `ShellChromePolicy`.
   - Android: rail at Compact height.
   - iOS: the bottom bar stays on iPhone held sideways, because that is the iOS convention. iPad regular width uses
     the sidebar or rail (iPadOS 18 tab bar and sidebar parity). Medium width on iPad (portrait 11", 834 pt) uses the
     rail.
6. **Navigation.** The JetBrains `navigation3-ui` 1.1.2 has no 1.2 APIs (`SceneDecoratorStrategy`,
   `SharedEntryInScene`) [U: whether `Scene` and `SceneStrategy` signatures are identical needs checking against
   1.1.2]. The `NavPane` scenes must use only `Scene`/`SceneStrategy`, which they do today.
7. **Tests.** Roborazzi `androidHostTest` renders the `commonMain` previews under `com.android.kotlin.multiplatform.library`
   (AGP 9) [V, #781]. iOS has no screenshot tool, so rely on the Android host, plus the macOS CI build and its static
   checks.

---

## 10. SwiftUI parity (rank 11, WP08)

```swift
// Shell: iPadOS 18 sidebar parity, iOS 17 fallback
struct TabShell: View {
    @State private var selection: AppTab = .home
    @State private var customization = TabViewCustomization()
    var body: some View {
        if #available(iOS 18, *) {
            TabView(selection: $selection) {
                ForEach(AppTab.primary) { tab in
                    Tab(tab.title, systemImage: tab.icon, value: tab) { tab.root }
                        .customizationID(tab.id)
                }
                TabSection("More") { ForEach(AppTab.secondary) { tab in Tab(tab.title, systemImage: tab.icon, value: tab) { tab.root } } }
            }
            .tabViewStyle(.sidebarAdaptable)          // bar on iPhone, top bar ⇄ sidebar on iPad
            .tabViewCustomization($customization)
        } else {
            TabView(selection: $selection) {
                ForEach(AppTab.all) { tab in tab.root.tabItem { Label(tab.title, systemImage: tab.icon) }.tag(tab) }
            }
        }
    }
}

// List-detail inside a tab: collapses to a stack in compact width
NavigationSplitView(columnVisibility: $visibility, preferredCompactColumn: $compactColumn) {   // iOS 17
    SampleListView(selection: $store.selectedID)
} detail: {
    NavigationStack(path: $router.detailPath) {
        if let id = store.selectedID { SampleDetailView(id: id) }
        else { ContentUnavailableView("Nothing selected", systemImage: "sidebar.left") }
    }
}
.navigationSplitViewStyle(.balanced)

// Supporting pane = inspector: trailing column in regular width, sheet in compact (iOS 17)
.inspector(isPresented: $showInfo) { InfoView().inspectorColumnWidth(min: 280, ideal: 320, max: 412) }
```

Rules:
1. **Width classes from geometry, not only from `horizontalSizeClass`** (§1.9).
   - At the root, `.onGeometryChange(for: WindowWidth.self) { WindowWidth(points: $0.size.width) } action: { width = $0 }`
     (iOS 16+).
   - Inject the result as `@Environment(\.appWindowWidth)`. Use the same 600/840/1200/1600 thresholds, in points.
   - `horizontalSizeClass` is still used for the system containers, which use it themselves.
2. **Input.**
   - `.keyboardShortcut` on toolbar buttons and `.commands { CommandMenu(...) }`. These feed the iPadOS menu bar and
     the ⌘-hold overlay [U: iPadOS 26 menu-bar behaviour from WWDC25 memory].
   - `.onKeyPress` (iOS 17) for screen keys.
   - `.hoverEffect(.highlight)` (iOS 17) on custom tappable rows.
   - `.contextMenu(menuItems:preview:)`, which right-click opens on iPad.
   - `.draggable` and `.dropDestination(for:)`.
3. **Resizing.**
   - No `UIRequiresFullScreen`, all four iPad orientations, and `UILaunchScreen`.
   - `.windowResizability(.contentMinSize)` with a min frame of 320×400.
   - `onInteractiveResizeChange` (iOS 26, behind `#available`) to defer expensive relayout until a drag-resize ends.
   - Never use `UIScreen.main` (deprecated, and wrong in multi-window); never use `UIDevice.current.userInterfaceIdiom`
     in features.
4. **Static checks (A12.4, `test_target_ios.py`):**
   - The plist keys above.
   - Ban `UIScreen.main`, `userInterfaceIdiom` in `Features/`, `.frame(width: N)` with N ≥ 320 in `Features/`, and
     `UIRequiresFullScreen`.
   - Require one `#Preview` with an iPad trait per feature view: `#Preview(traits: .landscapeLeft)` or a fixed layout
     of 1194×834 [U: the exact trait spelling].
5. **Snapshot tests** on the macOS CI: `swift-snapshot-testing` with `.image(layout: .device(config: .iPadPro12_9(.landscape)))`
   and iPhone configurations, at the same six logical sizes where the library offers them [U: version and device
   configuration names unchecked].

---

## 11. Foldables in detail (rank 9)

- **Posture source**, Android `MainActivity` (one per window):
  ```kotlin
  val posture by remember(activity) {
      WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).map { it.toPosture() }
  }.collectAsStateWithLifecycle(WindowPosture())
  // toPosture(): first FoldingFeature with state == HALF_OPENED → Tabletop (HORIZONTAL) or Book (VERTICAL);
  // hinge = bounds when isSeparating; FLAT and non-separating features are ignored (Pixel Fold inner display).
  ```
  This is the same rule as Compose's own `resolvePosture` [V] and Material's `calculatePosture` [V].
  `androidx.window:window` 1.5.0 is already on the runtime classpath through Compose UI (`ui-android` POM, scope
  runtime) [V]. Declare `androidx.window:window:1.5.1` in `:app` to compile against it, at no APK cost.
- **Tabletop behaviours** (requirement 23; Tier 1 `Foldables_Postures`):
  - `AppImageViewer` and any media screen put the image above the fold and the controls below it.
  - `AppSheet`'s medium detent rests on the fold line.
  - A list-detail screen may stack the list over the detail.
- **Book.** List-detail splits at the hinge (§4.5). `AppTwoPane` uses the hinge as its divider.
- **Continuity.**
  - Fold and unfold change the class: the scene changes, with no transition (`sameStack`).
  - With `configChanges`, the activity is not recreated: §5 and §6.4 test A.
  - Check whether the Pixel Fold's outer and inner screens differ in `densityDpi` [U; if they do, density stays a
    recreating change and §6.4 G covers it].
- **Tests.** Inject `WindowPosture` directly in Robolectric (§6.4 D). Then `adb emu posture 2` and `fold`/`unfold` on
  the Pixel Fold AVD (§6.4.3).

---

## 12. Self-adapting components (rank 8, WP03)

| Component | Rule by class |
|---|---|
| `AppSheet` | Compact width: a bottom sheet with detents (today's). Width ≥ Medium and height ≥ Medium: a **form sheet**, centred, `min(sheetMaxWidth, width − 2·gutter)` × `min(0.85·height, content)`, all corners rounded, scale-in using the alert spring, detents off, drag-to-dismiss off and a close button shown (iPad behaviour). Compact height: full height, capped width. |
| `AppActionSheet` | Compact: bottom (today's). Regular: anchored popover from the source rect, as on iPad (`AppMenu` style). |
| `AppMenu` / context menu | Already 'min..max' width. Anchor at the pointer for a right-click. Keyboard: arrow keys and Enter, Esc closes. |
| `AppAlert` | Fixed 300 dp (OK; iOS uses 270 pt). Never full width. |
| `AppToast` | Already capped at `sheetMaxWidth`. With the rail or sidebar, centre it over the content area, not the window. |
| `AppSection`, `AppRow`, settings | Inside `AppScaffold(content = Readable)`, apply `widthIn(max = readableMaxWidth)` centred, with full-width scrolling (§7.2). |
| `AppTopBar` / large title | Compact height: inline title. In a detail pane: no Back arrow at detail depth 1 (§4.3). Sidebar mode: the title starts after the sidebar. |
| `AppBottomActionBar` | In two panes, it spans only its pane, not the window. |
| `AppImageViewer` | Ctrl+wheel and pinch zoom; the tabletop split; keyboard ←/→ between images. |
| `AppDatePicker` | Regular width: two months side by side (optional). |
| `AppTabRow`, `AppSegmentedControl` | Widths come from content (already the case). Cap at `contentMaxWidth` and centre on desktop. |

---

## 13. Owner mapping (for the orchestrator)

| Item | Package |
|---|---|
| §2 adaptive layer v2, §7 input in `pressable` and components, §12 components, §11 posture types | WP03 |
| §3 chrome (rail fixes, sidebar, toggle motion), §4 panes and two-pane back, §5 navigator scope and manifest, §6.4 A/B/F/G tests, §8 scenarios | WP04 |
| §6.1 build-logic tasks and lint, §6.3 the screenshot convention plugin | WP02 (build-logic owner) |
| Generator assertions (§6.1.4), matrix rows for the screenshot and audit tasks | WP01 / WP09 |
| §9 CMP parity | WP07 |
| §10 SwiftUI parity | WP08 |
| §6.5 docs, AGENTS and the skill override notes | WP12 |

---

## 14. Sources

Primary, read today:
- Window size classes: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes ;
  androidx `window-core` 1.5.1 sources (Gradle cache) ; window releases
  https://developer.android.com/jetpack/androidx/releases/window
- Android 16 behaviour changes: https://developer.android.com/about/versions/16/behavior-changes-16
- Android 17 resizability: https://developer.android.com/blog/posts/prepare-your-app-for-the-resizability-and-orientation-changes-in-android-17 ;
  https://developer.android.com/about/versions/17/behavior-changes-17
- Material 3 adaptive releases: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive ;
  artifact `.module` files on dl.google.com ; androidx `PaneScaffoldDirective.kt`, `AndroidPosture.android.kt`
  (github.com/androidx/androidx)
- Desktop windowing: https://developer.android.com/develop/ui/compose/layouts/adaptive/support-desktop-windowing
- Configuration changes: https://developer.android.com/guide/topics/resources/runtime-changes
- Quality tiers: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality and `/tier-3`, `/tier-2`,
  `/experiences/desktop`, `/experiences/foldables`
- Googlebook: https://android-developers.googleblog.com/2026/09/adaptive-development-scale-app-googlebook.html
- Keyboard Shortcuts Helper: https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/keyboard-shortcuts-helper
- Emulator control: https://developer.android.com/blog/posts/emulator-control-for-adaptive-app-development
- Compose Preview Screenshot Testing: https://developer.android.com/studio/preview/compose-screenshot-testing ,
  `…-with-testsuites`, `…-release-notes`
- Roborazzi: https://github.com/takahirom/roborazzi (releases 1.69–1.76, issues #777 and #781) ; Paparazzi releases
  and issue #2491
- Nav3 recipes: https://github.com/android/nav3-recipes (`scenes/listdetail`, `navscenedecorator`)
- Compose sources (Gradle cache, 1.12.1 and 1.13.0-alpha03): `MediaQuery.kt`, `MediaQuery.android.kt`,
  `ComposeViewContext.android.kt`, `WindowInfo.kt`, `AndroidWindowInfo.android.kt`, `AndroidCompositionLocals.android.kt`,
  `TapGestureDetector.kt`, `RightClickGestures.kt`, `ScrollIndicator.kt`, `BasicTextField.kt`,
  `DeviceConfigurationOverride.android.kt`, `MultiPreviews.kt`, `Device.kt`, `PreviewWrapper.kt`, ui `lint.jar`;
  `navigation3-ui-android-1.2.0` bytecode
- JetBrains `compose-multiplatform-core` `jb-main`: `WindowContext.ios.kt`, repository tree (no iOS `UiMediaScope`) ;
  What's new in CMP 1.12: https://kotlinlang.org/docs/multiplatform/whats-new-compose-112.html
- Hilt components: https://dagger.dev/hilt/components
- Apple TN3192: https://developer.apple.com/documentation/technotes/tn3192-migrating-your-app-from-the-deprecated-uirequiresfullscreen-key ;
  SwiftUI API availability from developer.apple.com documentation JSON
- Google Android skills (local): `jetpack-compose/adaptive/SKILL.md` and references (mediaquery, grid, flexbox,
  tooling/debug, material-listdetail), `navigation/navigation-3/SKILL.md`

Secondary:
- WWDC26 session 278 transcript index: https://nonstrict.eu/wwdcindex/wwdc2026/278/
- fatbobman, size class vs available space: https://fatbobman.com/en/posts/from-size-class-to-available-space/
- M3 Expressive rail widths: https://9to5google.com/2025/05/14/material-3-expressive-navigation/
- SwiftUI after WWDC26: https://swiftwithmajid.com/2026/06/08/what-is-new-in-swiftui-after-wwdc26/
