# Visual QA round 1: WP02-M2 (generator Android target: tabs)

Full table: `DEFECTS.md`. The code is the integration branch's
`generator/genkit/targets/android/tabs.py`.

## QA-09 (S2). Seven tabs in one bar, "Settin…" truncated (REQ 18)

`regions()` (`tabs.py:25-29`) turns every entry in `spec.feature_modules` into a bar tab, between
Search and Profile. Nothing caps the count. With 2 scaffolded modules plus the 5 template tabs,
the bar holds 7 slots of about 53 dp each, and "Settings" is truncated.

The rule to implement, together with WP04's shell support (for-WP04 QA-09):

- At most 5 tabs show in the compact bar. With more, the shell shows the first 4 plus "More",
  as the iOS tab bar does. The HIG asks for 3–5 tabs; Compose's navigation bar guidance also
  says 3–5 destinations (https://developer.android.com/develop/ui/compose/components/navigation-bar).
- The generator orders tabs by priority:
  - Home first;
  - then the modules marked `tab: true`, in the order given;
  - then Feed, Search and Profile;
  - Settings goes to "More" first, because it is the least-visited tab and iOS apps commonly
    move it there.
- Emit the order into `AppDestinations`. When a spec yields more than 5 tabs, print a warning in
  `--dry-run` and in the headless JSON, so the website (WP10) can show it.
- Test it: a spec with 2 modules produces at most 5 bar tabs and a "More" root listing the rest.
  Run the matrix row with `orders` and `wallet`.

## QA-10 (S2). Every scaffolded module gets the same icon (REQ 18)

`shell_tab()` (`tabs.py:16`) hard-codes `icon = AppIcons.Grid` and passes no `selectedIcon`.
Orders and Wallet look identical.

Pick the glyph from the module name with a keyword map over WP03's `AppIcons`:

| Module name contains | Icon |
|---|---|
| order, shipment, delivery | `Package` |
| cart, shop, store | `Cart` |
| wallet, payment, billing, card | `CreditCard` |
| inbox, chat, message | `Message` (filled: `MessageFilled`) |
| mail | `Mail` |
| calendar, event, booking | `Calendar` |
| notification, alert | `Bell` (filled: `BellFilled`) |
| favourite, favorite, saved | `Heart` (filled: `HeartFilled`) or `Bookmark` (filled: `BookmarkFilled`) |
| file, document | `Folder` (filled: `FolderFilled`) |
| map, place, location | `MapPin` |
| explore, discover | `Globe` |
| photo, gallery | `ImageIcon` |
| team, contact, people | `Users` |

- Fall back by cycling through unused glyphs, `Grid`, `Tag`, `Bookmark`, `Star`, `Folder`, so
  that no two tabs share one.
- Always emit `selectedIcon`. Where a glyph has no filled form yet, WP03 adds one (for-WP03
  QA-06) or publishes the list of glyphs that have one.
- Honour an explicit per-module `icon` from the spec (for-WP01).
- Add a test that two scaffolded tabs never get the same icon.

Translations: "More" needs a string in every `generator/translations/android/<lang>/` file.
