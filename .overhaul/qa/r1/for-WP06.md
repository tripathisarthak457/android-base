# Visual QA round 1: WP06 (devtools, network recording, data)

Full table: `DEFECTS.md`. Code references are to the template as it stands on `wp/WP04` at
`d56ad13`; WP06 has not started, and the files are the same on the integration branch. All paths
are under `templates/android/`.

## QA-44 (S1). The DEV badge overlaps titles and the back button (REQ 30)

What you see:

- On the Redmi (`qa/ptr/frames/*`) the badge sits on top of the large "Samples" title.
- On the emulator it touches the title's cap height: a 3 dp gap at 2.625 px/dp.
- On pushed screens it sits directly on the back arrow (light 05, 06, 37, 38). It is clickable,
  so it can take the tap meant for Back.
- A dragged badge stays over the title (light 30).

The cause, in `core/devtools/.../DevBadge.kt`:

- `:62-63` and `:104` place the default 48 dp from the top. That ignores the real status-bar
  height and the screen's top bar.
- There is no edge snapping.
- `:91` sets `appClickable(minTouchTarget = 0.dp)`.
- `x` and `y` are computed in composition (`:64-65`), so a drag recomposes every frame.

The fix:

- Dock the badge to a screen edge, away from where titles, back buttons and the tab bar live.
  The default is the trailing edge at about 55 % of the height, drawn as a half-capsule flush
  with that edge.
- After a drag, snap horizontally to the nearer edge on a spring. Clamp it vertically between
  `safeDrawing` top plus `LocalTopChrome` and `LocalBottomChrome`. Persist the position.
- Read the offset only inside `Modifier.offset { }` or `graphicsLayer`.
- Give it a 48 dp touch target around a small visual, alpha 0.85 at rest.
- Hide it while the IME is up.

To verify, take shots of the root large title, a pushed screen with a back button, and the bar,
on the Redmi and on a 411 dp-wide emulator. Tapping Back where the badge used to sit must pop.

## QA-45 (S2). The badge's dark-mode contrast is 2.17:1

`DevBadge.kt:88` and `:98` draw white text on `colors.warning.content`, which is `#E9A23B` in
dark mode. Use the role's `on` colour, `colors.warning.on`, which resolves to dark ink in WP03's
palette. Or use `warning.subtle` with `warning.content` text. Light mode, white on `#A35B00`, is
5.18:1 and fine.

## QA-46 (S2). The inspector shows opaque bands under the system bars

Grey scrim bands sit above and below the panel: status bar and navigation bar (light 25 to 29,
dark 25, 28, 29). The badge peeks out under the panel's top-left corner.

The cause is `DevToolsPanel.kt:58-66`: a platform `Dialog(DialogProperties(usePlatformDefaultWidth = false))`,
which still fits its decor to the system windows.

The fix: add `decorFitsSystemWindows = false` and keep `background` outside `safeDrawingPadding()`.
This is the rule in the edge-to-edge skill (`android-skills/system/edge-to-edge/SKILL.md`,
§ Dialogs). Better still, present the inspector through WP03's overlay host as a large
`AppSheet`. That gives a grabber, the same scrim and predictive Back.

## QA-47 (S2). The inspector shows wrong data

1. The request body of a GET reads "e71" (light 26). `core/network/.../NetworkRecording.kt:31-34`
   uses `content::class.simpleName` for non-text bodies, and under R8 that is the obfuscated
   name of Ktor's `EmptyContent`. Use:
   ```kotlin
   requestBody = when (val content = request.content) {
       is TextContent -> content.text
       is OutgoingContent.NoContent -> null
       else -> content.contentType?.toString() ?: "binary"   // plus content.contentLength if known
   }
   ```
2. Every size reads "0 kB", and Stats reads "Received 0 kB" for 17 requests (light 28).
   `:36` and `:47-48` take the size only from `Content-Length`. Chunked and gzip responses from
   jsonplaceholder have none. Fall back to the decoded body you already read,
   `responseBody?.encodeToByteArray()?.size`, and label it "decoded". Show bytes below 1 kB:
   integer division (`DevToolsPanel.kt:159`, `:206`) floors to 0.
3. "0  (0%)" has a double space (Stats, Failed).

## QA-42 (S3). Feed excerpts keep hard line breaks

`data/feed/.../FeedPostDto.kt:18-19` maps `body = body.trim()`, so the feed card shows ragged
two-line excerpts (light 07, 08a, 08b). `data/sample`'s `SampleItem.preview` already collapses
`\n`. Add `FeedPost.excerpt` (whitespace collapsed) for the card, and keep `body` raw for the
expanded text. WP02-M3 switches the card to `excerpt`.
