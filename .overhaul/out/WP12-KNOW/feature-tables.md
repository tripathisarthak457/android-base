### Networking and data

| Feature | Key | In | What you get |
|---|---|---|---|
| Forced update and maintenance screens | `appstatus` | — | The server can require an update (HTTP 426, or a minimum-version header) or announce maintenance (503), and the app shows a full-screen message for either, whichever screen made the call. |
| Image loading (Coil) | `coil` | Lean, Standard | Remote images with a skeleton placeholder and a failure glyph. |
| App database (Room 3) | `database` | Standard | A Room 3 database with a table per domain, emptied when the person signs out. With the reference feature, its list is offline-first: what was fetched last is on screen at once, and a refresh writes the server's answer into the table. |
| REST networking (Ktor) | `network` | Lean, Standard | One HTTP client for the whole app: typed results, the session's token read for every request and refreshed one call at a time, every failure classified (offline, timeout, the server's own error code, an unreadable body), and the connection state as a hint a screen can show. |
| Offline request queue | `offlinequeue` | — | Changes made without a connection are queued and sent when one returns, by a WorkManager worker that waits for the network. Refusals are kept with the server's answer; rate limits and timeouts are retried. WorkManager carries a copy of Room of its own, so this adds more code than it looks. |
| Paged feed | `paging` | — | A Feed tab on Paging 3: pages load as you scroll, a failed page keeps what is already loaded and offers a retry, and pull to refresh reloads from where you are. The list component lives in core/ui for your own feeds. |
| WebSocket | `websocket` | — | One long-lived socket that is open while the app is in front: pings, a backoff that grows until a frame gets through, the new token after an account change, and a connection state a screen can render. |

### Sign-in and account

| Feature | Key | In | What you get |
|---|---|---|---|
| Biometric app lock | `applock` | — | A fingerprint, face or screen-lock prompt over the app at launch and after half a minute away, a setting to turn it on, and the app kept out of the task switcher's thumbnail while it is on. |
| Sign in, sign up and password reset | `auth` | Standard | Three screens against your auth endpoints, writing the encrypted token store — with server-side field errors mapped back onto the inputs that caused them. |
| Sign in with Google | `googlesignin` | — | A Continue with Google button on the sign-in screen through Credential Manager. The ID token goes to your backend, which answers with its own tokens like any sign-in. |
| Profile screen | `profile` | — | A Profile tab with an avatar, name and bio, saved on the device per user. With the media picker on, the avatar can be a photo. |

### Screens

| Feature | Key | In | What you get |
|---|---|---|---|
| Form state and validation | `forms` | Standard | Each field's text lives in a TextFieldState the ViewModel owns, so typing never waits on a state update and survives process death; validators return error codes the screen turns into copy, and server-side errors map back onto the fields that caused them. |
| In-app language picker | `language` | Standard | A Language row in settings. Uses the system's per-app language on Android 13+ so it also appears in the phone's settings, and applies it by hand on older releases. |
| Open source licences screen | `licenses` | — | The real dependency list, generated from the resolved classpath at build time and rendered by the design system. The build also fails on a licence the project has not allowed. |
| Onboarding flow | `onboarding` | Standard | A paged walkthrough shown once, recorded in the preference store. Skipping counts as finishing, so it never comes back on the next launch. |
| Reference feature | `sample` | Standard | A working list + detail feature against a live public API, with its tests. Proves every wire on first run; delete it once your own first feature exists. |
| Search screen | `search` | — | A Search tab: results as you type with a debounce, recent searches kept per user, and the empty, no-results and error states already drawn. |
| Settings screen | `settings` | Standard | Theme switch, analytics opt-out, sign-out with confirmation and the build version — wired to the preference store and the session. |

### Platform

| Feature | Key | In | What you get |
|---|---|---|---|
| In-app browser | `browser` | Standard | Web pages open in a Custom Tab coloured like the app rather than in the browser. Adds privacy and terms links to settings when settings is on. |
| Deep linking | `deeplink` | Lean, Standard | A custom scheme plus verified App Links. Each feature maps its own links to a screen; a link is held until onboarding, sign-in and the startup checks are done, opens on the tab that owns the screen, and is never replayed from Recents or after rotation. |
| Photo picker, camera and compression | `media` | — | The system photo picker and the camera app, with no permissions to ask for, and one image compressor that turns a picked photo upright, scales it and writes a copy the app owns. |
| Play in-app update and review prompts | `playstore` | — | A newer build offered once per launch through Play's own sheet and never offered again once turned down, a restart only when the person asks for it, and a rating prompt on a schedule Play honours. |
| Push notifications (FCM) | `push` | — | Firebase Cloud Messaging alone: notification channels, the permission asked for at the end of onboarding and offered again in settings, a tap that opens the app, a lock-screen-safe preview, and registration that is renewed only when it changes and removed at sign-out. |

### Observability

| Feature | Key | In | What you get |
|---|---|---|---|
| Analytics and crash-reporting seam | `analytics` | — | AnalyticsTracker and CrashReporter interfaces that write nothing until a vendor is bound, the settings opt-out applied to both, logged errors kept as breadcrumbs, and janky frames counted on the device. |
| Firebase Crashlytics | `crashlytics` | — | Crashlytics behind the crash-reporting seam: crashes and unhandled errors are reported, every logged error is a breadcrumb, and the R8 mapping is uploaded only by a release build that asks for it. |
| On-device inspector | `devtools` | Standard | A badge docked to the screen's edge in dev and debug builds, which can be dragged out of the way, and a panel behind it with the last 200 requests (credentials redacted), their timing and failure rate, and the build's details. Absent from a production build rather than hidden in one. |
| Firebase Analytics | `firebaseanalytics` | — | Firebase Analytics behind the analytics seam, switched off when the person opts out in settings. It brings the advertising-id permissions with it, which is why it is a choice of its own. |
| Feature flag seam | `flags` | Standard | Typed flags declared in one file with their defaults beside them, read or observed through an interface that resolves locally until a vendor is bound, and overridable by hand in the inspector. |
| Live performance overlay | `perfhud` | Standard | Frames per second at the display's real rate and the janky-frame share under the DEV badge, and a Performance tab in the inspector: frame times against the 60, 90 and 120 Hz budgets, frames per second over time, a main-thread time histogram, jank by screen, memory over time, start-up and request timings, and an optimisation score from 0 to 100 with a fix for every lost point. A structured logcat summary and an exported JSON and Markdown report. On in debug builds, a switch away in dev release builds, and absent from production builds; while it is off it puts nothing on the window. It lives in the on-device inspector, so it is on by default wherever the inspector is chosen, as in the Standard preset. |
| Firebase Remote Config | `remoteconfig` | — | Feature flags read from Remote Config: fetched at start-up, updated while the app is open when the console publishes a change, and fetched on every start in a debug build. |

### Design

| Feature | Key | In | What you get |
|---|---|---|---|
| Component catalog app | `catalog` | Standard | A second installable app showing every component and token, in the project's own design and motion style. A bar switches theme, style, motion, right-to-left, font scale and reduced motion live; each page opens directly with `--es section <Name>`, and a frame-time readout shows what each interaction costs. Depends on the design system alone, so design work rebuilds two modules. |
| Carousel, reorderable list and image viewer | `collections` | — | A paging carousel that peeks at its neighbours, a list whose rows lift and reorder under the finger or with accessibility actions, and a full-screen image viewer with pinch zoom and swipe to dismiss. |
| Data display | `datadisplay` | — | Stat tiles with a trend, a sparkline and a bar chart drawn in one pass each, key-value grids, a timeline, a data table with sticky totals and measured column widths, an animated counter and a star rating. |
| Specialised form fields | `formfields` | — | A one-time-code field, decimal and money fields that keep the caret where the user typed, a phone field with a dial code, a stepper, a tag field, a select field, a range slider and a step indicator for multi-page forms. |
| Downloadable Google Font | `googlefonts` | Lean, Standard | The typefaces the spec names (DM Sans and JetBrains Mono unless you choose others), downloaded through the Play Services font provider in exactly the weights the design style draws with. They are fetched while the splash is up; a launch where they take longer than 150 ms draws in the platform font throughout, so text never swaps under the reader. |
| Liquid glass foundation | `liquidglass` | — | The foundation liquid glass is drawn with, on Kyant's backdrop library: the glass tier the device can afford (live, live without the lens, frosted, solid), which battery saver, heat, a GPU that falls behind and Reduce transparency step down; the scene backdrop floating chrome samples; glass surfaces; and the travelling lens. No bar, sheet or other component draws with it yet: the project gets the pieces and Kyant's two libraries, and its look does not change. |
| Date and time pickers | `pickers` | — | An inline month calendar with limits and disabled days, iOS-style wheel pickers for any list, a time picker built from wheels, and compact date and time pills that open them in a sheet. |

### Build and quality

| Feature | Key | In | What you get |
|---|---|---|---|
| Continuous integration | `ci` | Lean, Standard | A GitHub Actions workflow that runs `check` (unit tests, lint, the code rules, the module graph and the Compose stability gate) and assembles the debug and release builds on every push and pull request, uploading the reports when something fails. |
| Coverage floor (Kover) | `coverage` | — | One merged coverage number for the whole project with a floor the build enforces, generated code excluded so the report is about code somebody wrote. |
| Dependency health report | `depsanalysis` | — | Names every module declaring a dependency it never uses, or using one it only gets transitively — the second being the failure that surfaces months later as an unrelated module breaking. `./gradlew buildHealth` writes the report; it is advice rather than a gate, because two of this template's own decisions produce advice it is right to ignore. |
| Formatting (Spotless + ktlint) | `formatting` | — | Spotless runs ktlint's Android Studio style over every Kotlin and Gradle file: `check` fails on a file that is not formatted and `spotlessApply` fixes it. The rules live in .editorconfig, which Android Studio reads too. |
| LeakCanary (debug only) | `leakcanary` | Lean, Standard | Memory-leak detection in debug builds. |
| Performance harness | `performance` | — | A macrobenchmark module (cold and warm start, tab switch, push and pop, predictive back, rapid Back, scroll) that runs against the dev flavour, the baseline profile generator, tools/perf (one command that installs a release build, measures every journey on a connected phone and fails on the frame-time gates) and tools/adaptive (the app across window sizes, folds, a second window and the keyboard on an emulator). Every project ships the checked-in baseline profiles without it. |

