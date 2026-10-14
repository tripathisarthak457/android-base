# Sheets

Every sheet in BaseApp is `appSheet`, from `DesignSystem`. It is the system's sheet: the card stack
behind it, the detent motion, the rubber band at the top, interactive dismissal, the keyboard and
VoiceOver are Apple's, and they behave as in every other app on the device. The design system maps
one set of options onto Apple's presentation modifiers and gives the sheet the design style's
surface. The Android template's `AppSheet` takes the same options, so a feature built for both
platforms reads the same on each; the table at the end lists where they differ, and why.

A feature never calls `.sheet`, `.presentationDetents` or `.interactiveDismissDisabled` itself. The
generator's checks fail on each.

## What there is

| Call | What it does |
|---|---|
| `.appSheet(isPresented:options:selection:onDismiss:content:)` | Presents `content` while the binding is true |
| `.appSheet(item:options:selection:onDismiss:content:)` | Presents a sheet per item; another item replaces it |
| `.appSheetPresentation(_:selection:)` | The same options for content something else presents as a sheet, such as an `.inspector` on a phone |
| `AppSheetOptions` | `detents`, `initialDetent`, `modality`, `contentInteraction`, `dismissible`, `material`, `placement`, `grabber` |
| `.appSheetHeader(_:)` | The title row: an inline navigation title and a close button |
| `.appSheetHeader(_:items:)` | The title row with toolbar items of the sheet's own (Cancel, Done) |
| `AppSheetCloseButton` | The header's close button, for a header with items of its own |
| `.appSheetFooter(_:)` | Actions pinned to the sheet's visible bottom, above the keyboard |
| `.appSheetDismissGuard(enabled:onAttempt:)` | Holds the sheet while there are changes and reports each attempt to close it |
| `.appSheetDismissConfirmation(isEnabled:onDiscard:)` | Asks "Discard changes?" before the sheet closes with changes in it |
| `.appActionSheet(isPresented:title:message:actions:)` | A few actions on what the person chose, with `AppSheetAction` |
| `.appChoiceSheet(isPresented:title:choices:selection:label:supportingText:)` | One choice from a list, the current one checked |

## Recipes

Each block is the whole call for a common sheet, and the SwiftUI form of the matching example in
the Android template's sheet documentation.

A quick sheet at full height, with a title and a close button:

```swift
.appSheet(isPresented: $showsFilters) {
    FilterList(store: filters)
        .appSheetHeader(Text("Filters", bundle: .module))
}
```

Half height that can grow, with a list that scrolls once the sheet is at full height:

```swift
.appSheet(isPresented: $showsPlaces, options: AppSheetOptions(detents: [.medium, .large])) {
    List(places) { place in
        PlaceRow(place: place)
    }
}
```

A short form sized to its content, asking before it discards a change. A fitted sheet is one plain
stack: its title is its first line and its action its last, and the keyboard lifts all of it.

```swift
.appSheet(isPresented: $renames, options: AppSheetOptions(detents: [.fitContent])) {
    VStack(alignment: .leading, spacing: AppSpacing.md) {
        Text("Rename", bundle: .module)
            .font(theme.typography.headline)
        RenameField(name: $name)
        Button(action: save) {
            Text("Save", bundle: .module)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.app(.filled, size: .large))
    }
    .padding(AppSpacing.gutter)
    .appSheetDismissConfirmation(isEnabled: name != original) { renames = false }
}
```

A taller form with a header, a sticky Save and the same question, which the close button asks too:

```swift
.appSheet(isPresented: $edits, options: AppSheetOptions(detents: [.large])) {
    EditListForm(store: store)
        .appSheetDismissConfirmation(isEnabled: store.state.hasChanges) { edits = false }
        .appSheetFooter {
            Button(action: save) {
                Text("Save", bundle: .module)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.app(.filled, size: .large))
        }
        .appSheetHeader(Text("Edit List", bundle: .module))
}
```

A sheet that is part of the screen, as Maps keeps its search: a peek that grows with Dynamic Type,
half and full height, and the screen behind usable up to half.

```swift
@ScaledMetric(relativeTo: .headline) private var peekHeight = 96.0

.appSheet(
    isPresented: .constant(true),
    options: AppSheetOptions(
        detents: [.height(peekHeight), .medium, .large],
        initialDetent: .height(peekHeight),
        modality: .nonModal(upThrough: .medium),
        dismissible: false
    )
) {
    PlaceSearch(store: search)
}
```

Glass while the sheet is partly open; the system turns it opaque at full height:

```swift
.appSheet(isPresented: $showsTracks, options: AppSheetOptions(detents: [.medium, .large], material: .glass)) {
    TrackList(store: tracks)
}
```

A custom detent, a tenth of the room but never shorter than a touch target:

```swift
struct SliverDetent: CustomPresentationDetent {
    static func height(in context: Context) -> CGFloat? {
        max(AppSizes.minTouchTarget, context.maxDetentValue * 0.1)
    }
}

AppSheetOptions(detents: [.custom(SliverDetent.self), .large])
```

Choices:

```swift
.appActionSheet(isPresented: $showsActions, title: Text("Groceries", bundle: .module), actions: [
    AppSheetAction(Text("Share", bundle: .module), action: share),
    AppSheetAction(Text("Delete List", bundle: .module), role: .destructive, action: delete),
])

.appChoiceSheet(
    isPresented: $picksLanguage,
    title: Text("Language", bundle: .module),
    choices: languages,
    selection: $language,
    label: \.displayName
)
```

A sheet that is its own screen, which Android registers as a navigation destination, is presented
by the view that opens it, with `appSheet(item:)`.

## Rules

- Pass a `Text` with `bundle: .module` wherever the sheet API takes a title or a label. A string key
  given to code in another package is looked up in the app's bundle and never found.
- `appSheetHeader` puts the content in a `NavigationStack`, so the content can push screens of its
  own. Apply it to the sheet's root content, last.
- `.fitContent` measures the content's ideal height, so fitted content is a plain stack: a
  `NavigationStack`, a `List` or a `Form` has no height of its own to fit, which rules out
  `appSheetHeader` and `appSheetFooter` in a fitted sheet.
- Without `initialDetent`, a sheet opens at its smallest detent, as SwiftUI's do; Android's open at
  the first listed. A feature on both platforms sets it.
- Move a detent from code inside `withAnimation(theme.springs.sheet)`, through `selection`.
- `appSheetDismissGuard` for your own question, `appSheetDismissConfirmation` for "Discard
  changes?". Enable either only while there is something to lose. Not `dismissible: false`, which
  gives no hook to ask from.
- One sheet at a time from the main interface. A sheet presented from a sheet stacks on it; close
  the first when the second replaces its task.
- No haptics at detents (iOS plays none), no drag gesture of your own on a sheet, and no background
  of your own: pick a `material`. From iOS 26 any background replaces the system's glass.

## Android and SwiftUI

"Same": both platforms behave alike. "Platform": each follows its platform, on purpose. "n/a": not
offered here, with the reason.

| Option or preset | Android | SwiftUI |
|---|---|---|
| Detents | `List<SheetDetent>`, default `[Large]` | `Set<AppSheetDetent>`, default `[.large]`: same. `Peek` is n/a: SwiftUI tells a detent nothing about the header, so use `.height` from an `@ScaledMetric` |
| Initial detent | `initialDetent`, default the first listed | `initialDetent`, default the smallest: same when set |
| Modality, `upThrough` | `SheetModality` | Same, through `presentationBackgroundInteraction`. Platform: a non-modal sheet covers the tab bar, stays over pushes and tab switches, and blocks other sheets from its screen |
| Content interaction | per gesture | Same mapping: `.automatic` resizes a modal sheet and scrolls a non-modal one |
| `dismissible` | yes | `interactiveDismissDisabled` |
| Guard, confirmation | `SheetDismissGuard`, `SheetDismissConfirmation` | `appSheetDismissGuard`, without a source (UIKit reports swipes and taps outside alike), and `appSheetDismissConfirmation`; iOS 27 asks with the system's `dismissalConfirmationDialog` |
| Close button | shown unless the person cannot close the sheet | `AppSheetCloseButton`, shown unless the sheet is not `dismissible` |
| `scrimTapDismisses` | yes | n/a: UIKit ties a tap outside to interactive dismissal |
| `dragEnabled` | yes | n/a: SwiftUI has no switch that holds a sheet on its detent |
| `recedesPresenter` | yes | n/a: the system owns its card stack |
| `contentUnderGrabber`, `containerBrush` | yes | Platform: the grabber always draws over the content |
| `keepContent` | yes | n/a: SwiftUI owns the presentation's lifetime |
| Material | Automatic is the design style's | Platform: `.automatic` is the system's, Liquid Glass from iOS 26 and the elevated surface before |
| Placement | Automatic, Bottom, Center, Start, End | `.automatic`, `.center`, `.leading`, `.trailing` from iOS 27; Bottom is n/a (the phone's only form; an iPad offers none) |
| Transition | Slide, CrossFade | Platform: the system's slide |
| Keyboard | Lift or Expand | Platform: the system lifts the sheet |
| Grabber | slot | `presentationDragIndicator` |
| Header, footer | slots | `appSheetHeader`, `appSheetFooter`, not in a fitted sheet |
| Pane title | `paneTitle`, the header's title | Platform: VoiceOver reads the navigation title |
| `onDetentChange` | settles the person made | Platform: `selection` sees every change |
| `onHidden` | after the exit | `onDismiss` |
| Haptics | a tick on arriving at a detent | none: iOS plays none at detents |
| Colours, radius, inset, shadow | `AppSheetStyle` | `presentationBackground` and `presentationCornerRadius` before iOS 26; the system's from iOS 26 |
| Action sheet | a sheet; a card anchored to its source from medium width | `appActionSheet`: the system's confirmation dialog, a popover from its source on an iPad |
| Choice sheet | fits up to 60 % and grows, search from six | `appChoiceSheet`: half and full height (a `List` has no height to fit), search from six |
| Destination sheets | `Presentation.Sheet(SheetSpec)` | n/a: features present from their views with `appSheet(item:)` |
| Inspector as a sheet | later | `appSheetPresentation` |
| Springs | `springs.sheet` | programmatic detent changes in `withAnimation(theme.springs.sheet)` |
