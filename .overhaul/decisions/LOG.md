# Decision log

Every decision taken without the user (the user, Oct 18: "log every decision with its reasoning so I can review it later").
Newest last. Format: date time | decision | options considered | reasoning | advised by.

- 2026-10-18 14:40 | Package HUD (not WP06) moves `:core:devtools` out of prod variants | WP06 fixes it before its merge; HUD does it | HUD builds on `wp/WP06` and is running now; WP06 is accepted and waiting, a second writer on the same wiring would collide | orchestrator (Opus)
- 2026-10-18 15:15 | The 45% one-window cap stops at 41% and adds 7-minute checks from 28% | stop at a fixed lower number; check every 20 minutes | usage rose 12-15 points per 20 minutes with three workflows, so a 20-minute check would overshoot 45% | orchestrator (Opus)
- 2026-10-18 16:55 | The user's scroll bug is fixed alone, the other workflows stay paused | fix inside WP03's run | the user's standing rule "a bug I report comes first"; WP03's run was paused anyway | orchestrator (Opus)
- 2026-10-18 23:15 | Sonnet does only the HUD demo's device capture; an Opus verifier writes SHOW.md and a separate Opus reviewer checks it | Sonnet for whole agents; no Sonnet at all | the user's working rules; the capture is long, mechanical and token-heavy, while judging the overlay and its overhead needs Opus; the step had not run yet, so the change costs no finished work | orchestrator (Opus)
- 2026-10-19 03:17 | The back-gesture texture patch (`out/WP04-perf/backcard-texture.patch`) stays uncommitted until the user decides | commit it now on the agent's recommendation | it breaks a written rule (A11.3 rule 1) and trades about one vsync of follow latency and about 10 MB GPU for fewer hitches; a rule exception is the user's to grant, not a design choice covered by the pre-approval | orchestrator (Opus)
