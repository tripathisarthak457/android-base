## Orchestrator note (Oct 23, 22:09)

The user answered media.md 9.2 (self-host the sample clips as a release of the public android-base repository) and 9.3 (downloads into %TEMP%\mrev\ are approved). L5, L6, V4 and phone session 2 are unblocked. Prepare and review the clip set; when it is ready, add a line "Orchestrator requests: create release sample-media-v1 with <files>" at the top of this handoff and continue with what does not need the URLs; the orchestrator publishes it (never an agent).

# MEDIA handoff (staging-wp/MEDIA): images and video, Telegram P3/P7/P9, District P3

## Progress

- Run 1 started 2026-10-23 21:51 IST. Branch `staging-wp/MEDIA` at staging `d429cbfb` (merge of staging: up to date).
- Order chosen (logged below under Decisions): `:core:image` first (media.md open point 10.1 allows it), then P3's
  loader, rule and fade into it, P7, District P3, then media.md's G1, M2, M3, M4, G2, V0-V4, P1a/P1b, D1.
- Next step: M1 (`:core:image`).

## Decisions (run 1)

1. media.md open point 10.1: `:core:image` (M1) lands before P3's image commits; P3's loader is written in
   `core/image/.../AppImageLoader.kt` with media.md's optional-client signature from the start, so P3's
   `<opt:coil&!network>` loader variant never exists (its test becomes 6.13). Reason: both plans land on this one
   branch; building P3 in `:app`/`:core:ui` only to move it two commits later is churn with no reviewer benefit.
2. media.md 9.1 and 9.2 take their recommended answers (REQUIREMENTS 55(d)): Coil off by default and in Everything only;
   sample videos hotlinked from Wikimedia Commons (CC0).
3. `T/docs/PERFORMANCE.md` does not exist on staging (WP12's docs base is not merged). Docs that the plans put there are
   written into the feature-owned `docs/IMAGES.md` / `docs/VIDEO.md` and the template's `AGENTS.md`; the
   PERFORMANCE.md hunks wait for WP12's base (recorded under "Waiting").

## Waiting on staging's open fix or on others

(filled as steps are reached)
