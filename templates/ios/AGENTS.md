# Working on BaseApp

Instructions for AI coding agents and people. BaseApp is a SwiftUI app for iPhone and iPad, iOS 17
and later, built from local Swift packages. These rules keep it smooth at 120 Hz and adaptive on
every window size; follow them in every change.

## Build and verify

```bash
xcodegen generate                                   # after any change to project.yml or a new file outside a package
xcodebuild -project BaseApp.xcodeproj -scheme BaseApp -destination 'platform=iOS Simulator,name=iPhone 17' build test
(cd Packages/Core && xcodebuild -scheme Core-Package -destination 'platform=iOS Simulator,name=iPhone 17' test)
(cd Packages/Features && xcodebuild -scheme Features-Package -destination 'platform=iOS Simulator,name=iPhone 17' test)
```

Judge smoothness only on a Release build on a device, with Instruments' SwiftUI and Animation
Hitches templates. Debug builds are many times slower and say nothing about frame times.

## Layout and dependency rules

```
BaseApp/                       composition root (AppServices), window state (AppModel), tabs, commands
Packages/Core/Sources/
  AppCore/                     AppResult, AppError, LoadState, TaskBag, AppClock, Log
<!-- <opt:liquidglass> -->
  DesignSystem/                Tokens (generated), Theme, Foundation/{Interaction,Adaptive,Glass}, Components/<Family>
<!-- </opt:liquidglass> -->
<!-- <opt-else:liquidglass> -->
<!--~   DesignSystem/                Tokens (generated), Theme, Foundation/{Interaction,Adaptive}, Components/<Family> -->
<!-- </opt-else:liquidglass> -->
  Navigation/                  Router, Shell (TabShell), DeepLinks
<!-- <opt:network> -->  Networking/                  APIClient, Endpoint, NetworkMonitor
<!-- <opt:database> -->
  Persistence/                 Settings (SettingsRepository), Database (SwiftData)
<!-- </opt:database> -->
<!-- <opt-else:database> -->
<!--~   Persistence/                 Settings (SettingsRepository) -->
<!-- </opt-else:database> -->
Packages/Features/Sources/
  <Name>Data/                  API/ (protocol, models), Remote/ (+ DTO/), Local/, Default<Name>Repository.swift
  <Name>Feature/               one folder per screen or flow (List/, Detail/, Overview/, Intro/), Components/, Resources/
```

- A feature depends on its own `<Name>Data` and on Core products. Never on another feature.
- A data target never depends on a feature. Cross-domain rules get their own data target.
- Folders are named for a screen or a concept, never a layer: no `Views/`, `ViewModels/`, `Stores/`,
  `Utils/`, `Helpers/`, `Common/`. A store sits beside its view (`List/<Name>ListStore.swift` next to
  `List/<Name>ListView.swift`). One public type per file.
- `Tokens/DesignTokens.swift` is generated from the generator's `tokens/*.json`. Never edit it.
  `BaseApp/Assets.xcassets/AccentColor` is generated with it: the same accent, as the app's
  default tint for whatever the theme does not reach. A new brand colour changes both.

## Concurrency (Swift 6)

- UI targets (`DesignSystem`, `Navigation`, every `<Name>Feature`, the app) are main-actor by default.
<!-- <opt:network> -->
- `AppCore`, `Networking`, `Persistence` and every `<Name>Data` are nonisolated. Never write
<!-- </opt:network> -->
<!-- <opt-else:network> -->
<!--~ - `AppCore`, `Persistence` and every `<Name>Data` are nonisolated. Never write -->
<!-- </opt-else:network> -->
  `@MainActor` in them; the generator's checks fail on it.
- An async function runs on its caller's actor unless it is `@concurrent`, and a store's caller is
  the main actor. Decoding, mapping and anything heavy is `@concurrent`; database work goes through
  a `@ModelActor` per domain, never a `ModelContext` on the main actor.
<!-- <opt:network> -->  `APIClient.decode` and each data target's `Remote/<Name>API.swift` show it.
- A value type that crosses actors declares `Sendable` in its own declaration.
- In a main-actor target, a value type that nonisolated code reads (an environment key's default,
  a geometry callback, anything passed where `Sendable` is required) is declared `nonisolated`:
  `public nonisolated struct AppTheme`. Otherwise its initialisers and conformances belong to the
  main actor and those callers do not compile.

## Stores (`@Observable` MVI)

```swift
@MainActor
@Observable
final class OrdersStore {
    struct State: Equatable, Sendable { var load: LoadState = .idle; var query = ""; var orders: [Order] = [] }
    enum Event: Sendable { case appeared, retry, queryChanged(String) }

    private(set) var state = State()
    private let repository: any OrdersRepository
    private let tasks = TaskBag()

    func send(_ event: Event) {                        // never async: a tap is never held up by I/O
        switch event {
        case .appeared, .retry:
            tasks.latest("load") { [weak self] in await self?.load() }
        case .queryChanged(let query):
            state.query = query
            tasks.latest("load", debounce: .milliseconds(300)) { [weak self] in await self?.load() }
        }
    }

    private func load() async {
        state.load = .loading
        let result = await repository.orders(matching: state.query)
        guard !Task.isCancelled else { return }
        switch result { case .success(let orders): … case .failure(let error): state.load = .failed(error) }
    }
}
```

- Work starts only through `TaskBag`: `latest` for loads and searches (a new one cancels the old),
  `exclusive` for submits and payments (a second tap is ignored while one runs) and for a stream a
  screen follows, so appearing again does not subscribe twice.
- Repositories return `AppResult`; stores switch over every case. Never `try!`, never `print`.
- No `ObservableObject`, no `@Published`, no `DispatchQueue.main.async`.
- Navigation is the view's: a list's selection is bound to the tab's `Router`, and a store takes
  the `Router` only when its own work decides where to go (push the order a submit created).
  Show transient messages through `ToastCenter`.
- `.refreshable` calls an `async` method that awaits the store's own task (every list store's
  `refresh()`).

## Navigation

- Each tab has a `Router`: `path` for its `NavigationStack`, `selection` for a list-detail split view.
- Push with `router.push(route)`, register screens with `.navigationDestination(for:)` in the
  feature's root view. Tapping the selected tab calls `router.popToRoot()`.
- List-detail screens use `NavigationSplitView`: the list's selection is `router.selection` (the
  item's id as a string), the detail column loads the selected id through its own store, and an
  `AppStateView` (`ContentUnavailableView`) stands in while nothing is selected. On a phone the
  selection alone decides which column shows; never bind `preferredCompactColumn` to state of
  your own.
<!-- <opt:sample> -->  `SampleRootView` is the reference.
- Deep links are `<scheme>://<tab>/<id>`; `AppModel.open(_:)` selects the tab and the item.

## Design system

- Read the theme with `@Environment(\.appTheme)`: `theme.colors`, `theme.typography`,
  `theme.shapes`, `theme.springs`. Spacing and sizes are `AppSpacing.*` and `AppSizes.*`.
- Never a raw colour, font size, duration or spacing in a feature.
- Type is Apple's text styles with the design style's weights, so Dynamic Type works everywhere.
  Test every screen at the largest accessibility size.

| Control | Press feedback |
|---|---|
| Filled, tinted and destructive buttons, chips, cards | `.buttonStyle(.app(…))` or `.scalePress` |
| Plain buttons, icon buttons, toolbar actions, links | `.app(.plain)` or `.dimPress` |
| Tappable rows outside a `List` | `.highlightRow` |
| Rows in a `List`, tabs, segments, toggles | the system's own |

| Moment | Haptic (`.appHaptic(_:trigger:)`) |
|---|---|
| Plain taps | none |
| Segment, picker, page or slider step | `.selection` |
| A pull or swipe crossing its threshold | `.impact(weight: .light)` |
| An outcome | `.success` / `.error` (toasts play them for you) |

## Motion and performance

Use the system's own motion; never rebuild it:

| Motion | Use |
|---|---|
| Push, pop, interactive back swipe | `NavigationStack` |
| Zoom into a detail (iOS 18) | `.navigationTransition(.zoom(sourceID:in:))` + `.matchedTransitionSource`, behind `#available` |
| Sheets with detents and the card stack | `.appSheet(…)` / `.presentationDetents` |
| Context menu with a preview | `.contextMenu(menuItems:preview:)` |
| Pull to refresh, swipe actions | `.refreshable`, `.swipeActions` |
| List insert, delete, move | `.animation(theme.springs.layout, value: …)` or `withAnimation` around the change |
| Tab bar minimize (iOS 26) | already on in `TabShell` |
| Symbol effects | `.symbolEffect(.bounce, value:)` |
| Scroll-linked effects | `.scrollTransition`, `.onScrollGeometryChange` |
<!-- <opt:liquidglass> -->
| Glass (iOS 26) | `.appGlass(in:)` inside `AppGlassGroup`, on floating chrome only |
<!-- </opt:liquidglass> -->
<!-- <opt-else:liquidglass> -->
<!--~ | Glass (iOS 26) | the system bars, sheets and tab bar have it already; add none of your own | -->
<!-- </opt-else:liquidglass> -->

- Every custom animation uses a spring from `theme.springs`. SwiftUI springs retarget from their
  current velocity, so interrupting one never jumps.
- Read state narrowly: pass a row its item, not the store. No `AnyView`.
- To count body evaluations, add `let _ = Self._printChanges()` to a body while debugging, then
  remove it; for real numbers use Instruments' SwiftUI template on a Release build.
- `CADisableMinimumFrameDurationOnPhone` is on in `project.yml`; without it ProMotion iPhones run
  custom animations at 60 Hz. Keep it.
- Never `.blur`, `.shadow` or glass on rows of a scrolling list; never a repeating animation that
  ignores Reduce Motion or keeps running off screen.

## Adaptive layout (iPhone, iPad, resizable windows)

- Decide layout from `@Environment(\.appWindowWidth)` (compact < 600 ≤ medium < 840 ≤ expanded
  < 1200 ≤ large < 1600 ≤ extraLarge points). Do not decide from `horizontalSizeClass` alone, the
  device or the orientation: an iPhone app in a wide iPad window stays compact in size class.
- Never `UIScreen.main`. Never `UIDevice.current.userInterfaceIdiom` in a feature.
- No fixed or minimum widths of 320 points or more in a feature: the narrowest window is 320.
  Cap content instead and centre it: `.frame(maxWidth: AppLayout.readableMaxWidth)` for running
  text, `AppLayout.formMaxWidth` for a form or a single flow, then `.frame(maxWidth: .infinity)`.
  The scroll view around it stays window-wide, so it scrolls from anywhere.
- List-detail: `NavigationSplitView`. A side panel: `.inspector` (a sheet on a phone).
- Every screen works with a keyboard: `.keyboardShortcut` on toolbar actions, `.onKeyPress` for
  screen keys, Esc (`.cancelAction`) closes or skips, Return (`.defaultAction`) confirms or goes on.
  Custom tappable rows get `.hoverEffect`.
- Every `…View.swift` in a feature has a `#Preview` at an iPad size:
  `#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834))`. Preview data lives in the
  feature's `Previews/` folder; that folder and every `#Preview` that uses it sit inside
  `#if DEBUG`, so a release build carries none of it.
- Never add `UIRequiresFullScreen` or remove an orientation from `project.yml`.

## Strings

Every string a person reads is in the target's `Resources/Localizable.xcstrings`, looked up with
`Text("…", bundle: .module)` or `String(localized: "…", bundle: .module)` inside a package, and with
a plain literal in the app target. Text from the server is shown as is: `Text(verbatim:)` or
`Text(someString)`. A new key needs a translation for every language in `CFBundleLocalizations`
(`project.yml`); Xcode's catalog editor shows which are missing.

The type is the system font (SF Pro) at the design style's weights; a custom typeface is a font
file in the app target and a `Font.custom(_:size:relativeTo:)` in `AppTypography`, so Dynamic Type
still scales it.

## Privacy manifest

`BaseApp/PrivacyInfo.xcprivacy` declares every required-reason API the app's code calls: today
`UserDefaults` and `@AppStorage`, with reason `CA92.1`. App Store Connect refuses a build that calls
one without declaring it, so code that starts using another category (file timestamps, system boot
time, disk space, active keyboards) adds the category and Apple's reason code there in the same
change. Data the app collects goes under `NSPrivacyCollectedDataTypes` in the same file.

## Checklists

**New screen:** loading, empty, failure and offline states; dark appearance; the largest Dynamic Type
size; right-to-left; an iPhone and an iPad `#Preview`; a store test with a fake repository.

**New feature:** `<Name>Data` and `<Name>Feature` targets and their test targets in
`Packages/Features/Package.swift`; the feature's product in `project.yml`; a case in `AppTab`
if it is a tab, with an SF Symbol no other tab uses; strings in every shipped language.

**Before saying done:** `xcodegen generate`, then the three `xcodebuild … test` commands above,
all green, or the exact failure reported. For anything that moves, a Release build on a device
with Instruments' Animation Hitches template, and the hitch rate in your summary.

## Never

`ObservableObject` · `@Published` · `try!` · `print(` · `DispatchQueue.main.async` · `AnyView` ·
`UIScreen.main` · `@MainActor` in a data target · a `ModelContext` on the main actor · a raw colour,
size or duration in a feature · a custom push or sheet transition · glass inside a scrolling list ·
editing `DesignTokens.swift` by hand.
