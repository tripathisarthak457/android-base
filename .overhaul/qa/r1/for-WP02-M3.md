# Visual QA round 1: WP02-M3 (reference feature screens on the new design system)

Full table: `DEFECTS.md`. Code references are to `wp/WP04` at `d56ad13`, which carries the
current feature code with WP04's chrome edits, under `templates/android/feature/`.

These screens move onto WP03's design system in M3. Each item below is something the migration
must change, not just restyle.

## QA-35 (S1). Root headers are static and the list is clipped hard under them

Home, Feed, Search and Settings put the large title, subtitle and search field in
`AppScaffold(topBar = …)`. The old `AppScaffold` (`core/designsystem/.../AppScaffold.kt:63-65`)
stacks `topBar()` above the content, so scrolled content is cut by a hard horizontal edge (light
02, 08a, 08b, 13, 21; dark 02, 08b, 13). There is no collapse and no edge effect.
`sample/list/SampleListScreen.kt:52-71` is the pattern to replace.

The fix:

- Use WP03's `AppScaffold`, where content runs under the bar, with `AppLargeTitleTopBar(title,
  subtitle, scrolled = { listState.scrolledFromTop() })` and `Modifier.scrollEdgeFade`.
- Put the search field in the bar's accessory slot, which WP03 adds (for-WP03 QA-35). Use the
  list's first item until then.
- Every list pads itself with `chromeContentPadding()`.

To verify, scroll each root screen in light and dark. Content must fade under the bar, and the
large title must hand over to the small centred title.

## QA-36 (S2). Detail repeats its title, and the bar is opaque at rest

`sample/detail/SampleDetailScreen.kt:40-41` uses `AppBackTopBar(title = item.title)` above an H1
with the same text, and the bar is solid white over a grey page (light 05, 06).

Use `AppTopBar(title, onBack, scrolled = { … })`. The bar is clear at the top. The inline title
fades in only after the H1 scrolls under the bar; drive its alpha from the H1's position in
`graphicsLayer`.

## QA-37 (S2). The detail push flashes a placeholder and a skeleton

The incoming screen first shows the title "Detail" (`R.string.sample_detail_title`) and a
skeleton. It switches to the real title and body mid-slide
(`light/41-push-pop-transition-sheet.png`, frames 2 to 5).

Pass what the list already has, the title and preview, in the key (`SampleDetailKey(id, title)`)
or through a repository memory cache read synchronously at construction. The first composed frame
is then final, and the push's first frame is cheaper too. Load only the extra fields
asynchronously.

## QA-38 (S2). Settings is a stack of cards

Today every setting is its own `AppCard`, under heavy bold section headers
(`settings/Settings.kt:242-414`). Haptic feedback uses the notification bell (`:271`). The
version is in `AppMonoText` (`:413`). Spacing is uneven: 34 px under the title, 48 px between
sections.

Rebuild it as iOS-style inset-grouped `AppSection`s with `AppRow` and `AppToggleRow`, using
footnote section headers (`AppSectionHeader`).

- Appearance: theme segmented control, Haptics, Language.
- Privacy.
- Account: Sign out, destructive.
- Legal.

Put the version in the last section's footer, in `footnote` and `contentTertiary`. Pick a haptic
glyph that is not a bell.

## QA-14 (S2). The language picker shows no current value

Rebuild it on `AppSheet(title = …, showCloseButton = true)` with single-choice rows: the
`AppRow(selected = …)` checkmark WP03 adds. Tapping a row applies the language and closes the
sheet. Drop the full-width Cancel pill: the grabber, the close button and Back already dismiss.

## QA-22 (S2). Pull to refresh while offline gives no feedback, and nothing says offline

A pull on Home while offline settles back silently. The cached list shows no offline cue
(light 31, 34).

- When a refresh fails, `showMessage(UiText.of(R.string.…offline…), MessageKind.Warning)`.
- When the stream serves cached data offline, show an `AppBanner` above the list
  ("Showing saved posts"), dismissed once the network returns.

## QA-39 (S3). Offline copy

The title and message both say "You are offline". The message is `core/ui`'s
`common_error_offline`, "You are offline. Check your connection and try again." Make it "Check
your connection and try again."

The action is "Retry" on Feed (`feed_retry`) and "Try again" on Search (`search_retry`). Use one
label app-wide: the design system's `designsystem_try_again`. Update `generator/translations/*`.

## QA-40 (S2). Sign-in polish

- "Continue with Google" has no Google "G". Google's branding guidelines require it: "The button
  must always include the standard color for the Google 'G'"
  (https://developers.google.com/identity/branding-guidelines). Use the official asset at the
  documented size and padding.
- "Create account" and "Forgot password" are inset from the margins (light 35). With WP03's
  `AppLinkButton`, align them to the text column.
- The disabled "Sign in" gives no reason, and "not-an-email" is not flagged when focus leaves the
  field (light 36). Validate on blur and show the field error. Keep the button enabled, and
  validate on tap if you prefer the iOS pattern.

## QA-41 (S3). Profile: Save is half-hidden under the keyboard

While editing, "Save changes" sits as a sliver at the keyboard's top edge (light 17, dark 17).
Either pin the action in WP03's `AppBottomActionBar` (it rides above the IME), or bring it into
view on focus with `BringIntoViewRequester` on the field plus the button.

## QA-43 (S3). Search does not highlight matches

Bold or accent-colour the matched substring in the result title and excerpt (light 12, 13). Use
an `AnnotatedString` built in the ViewModel, not in composition.

## QA-28 and QA-42 (S3, data). Feed cards

- Use `FeedPost.excerpt` from WP06 (whitespace collapsed).
- "Author %1$d" (`feed/src/main/res/values/strings.xml:5`) makes the avatar initials "A1".
  WP03's initials fix gives "A". Consider names from jsonplaceholder `/users`, so the avatars
  show real initials and varied colours.
