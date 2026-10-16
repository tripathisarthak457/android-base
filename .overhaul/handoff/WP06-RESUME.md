# WP06 resume brief

**Superseded on Oct 15, 16:20 by `handoff/WP06.md`, "Progress", run 10.** Everything in sections 1 to 3 below that
could be done without WP04 and WP03 is done: the gate test is committed (`a8129c4`, the `onExit` mutation now
caught), run 9's phone results are recorded, the integration branch (WP10) is merged (`427dce4`), and the K13
device check is written (`badge_qa_check.py --top-chrome`, failing on today's build as expected). Start from
the run 10 entry's "Next run" list.


## 1. Exact state
- Worktree `wt/WP06`, branch `wp/WP06`, HEAD `1dcacd9` (12:50, badge drag reads latest bounds); 30 commits over `overhaul/multiplatform-v2`, not merged.
- Uncommitted (run 9 work, not yet in Progress): `core/applock/build.gradle.kts` (+Robolectric, ext.junit, compose ui-test-junit4, `isIncludeAndroidResources`) and new `core/applock/src/test/.../AppLockGateTest.kt` (186 lines, 4 tests). Looks finished: closes the Open issue "No UI test for the app-lock cover".
- `AppLockGate.kt` is clean in git (mutation runs restored it, last at 18:13). Check `git status` shows it unmodified before anything else.
- Tests: `out/WP06/logs/r9t2.log` 3/3 green (17:40); `r9mut3.log` (18:13): all 4 tests pass on the committed gate, and every mutation (no pointerInput, no onEnter cancel, no clearAndSetSemantics, cover not asking focus) is caught except "no onExit cancel" (survives: harmless or a missing test, decide).
- `logs/accept13.log` (18:03): ACCEPT OK at `1dcacd9` on the phone 9a7c74f4: 255 generator tests, consistency, catalogue, BUILDS OK stamp, BADGE DRAG OK (moved -948,-773), BADGE QA OK, DEVICE CHECKS OK. Not yet recorded in WP06.md Progress / Measurements / Open issues (Redmi item is now done).

## 2. Still owed (checklist)
- [ ] Commit the AppLockGate test (build file + test) after a targeted green run; decide on the surviving "no onExit cancel" mutation (add a Tab-out-of-cover test, or note it as belt-and-braces).
- [ ] Record run 9 in `handoff/WP06.md`: Progress, Measurements "Device checks" (phone numbers from accept13), scope row QA-44 ("Redmi waits" is stale), Open issues (drop the Redmi and app-lock UI-test items); mark the Redmi item in `followups.md` done.
- [ ] REQ 34 / K13 / AUDIT C-8: badge top bound = the front screen's whole top chrome (large title + search accessory), from WP04's window-wide published value, read only in `bounds()`, re-clamped when it changes. Acceptance: `badge_qa_check.py` drags and throws to the top on Samples root (search field) and a pushed screen; badge never intersects title, search or Back; phone and emulator. If WP04 does not publish it: keep today's clamp and add an Open issue.
- [ ] REQ 33 / SHEET-SPEC §19.8 (C2-38): DevTools panel off its `Dialog` and into an `AppSheet` (`[Medium, Large]` on a phone, modal `End` side panel from Expanded; D-22: no Dialog or Popup). This also closes the followup "windows above the app-lock cover" for the panel.
- [ ] REQ 35: nothing owned (lens = WP05, WP11, WP07-B, QA2). REQ 36: WP06 is part of the Android "100% done" bar (security; the intent and permission audits are done per the scope table). It merges 4th; WP02-M2's database scaffolding waits on it; the full Android matrix must be green after the merge.
- [ ] Carried and routed, not blocking: JankStats `nav=` tags after WP04 (followups, WP13); fakes in `testFixtures` after WP02-M2; "top recomposers" panel not built (Deviation).

## 3. Next three steps
1. `git -C wt/WP06 status`: `AppLockGate.kt` must be clean. Under the Gradle lock, run `:core:applock:testDebugUnitTest` plus app lint and compose stability, then commit `templates/android/core/applock/build.gradle.kts` and `.../src/test/kotlin/com/base/app/core/applock/AppLockGateTest.kt`. It is a test-only commit, so the device results at `1dcacd9` still hold, but the `builds-ok` stamp will not match the new commit.
2. Edit `handoff/WP06.md` (Progress run 9, Measurements, scope table, Open issues) and `followups.md` (WP06 Redmi item). Then report "waiting" for WP04 and WP03.
3. After WP04 and WP03 merge, bring `wp/WP06` up to date with `overhaul/multiplatform-v2` (now `a402d87` with WP10; no file overlaps WP06's diff). Then do K13 in `templates/android/core/devtools/src/main/kotlin/com/base/app/core/devtools/badge/DevToolsOverlay.kt` `bounds()` (the `top =` line, about :103) and extend `out/WP06/badge_qa_check.py`. Then do §19.8 in `.../devtools/panel/DevToolsPanel.kt`. Finish with `out/WP06/accept.sh` on 9a7c74f4 and on emulator-5554.

## 4. Waiting on
- WP04 merge: the window-wide front-screen top chrome (WP04.md item 4, "Seam for WP06") for K13; the `ShellObserver` seam for the JankStats tags.
- WP03 merge (with WP03-skel): `AppSheet`, the overlay host and `LocalTopChrome` for §19.8. Merge order is WP10 (done), WP04, WP03, WP06.
- Phone 9a7c74f4: has passed at `1dcacd9`; needed again for the K13 acceptance after the rebase. The adb lock is shared with the other packages' phone gates.
- CP2 (the glass decision, WP05): no WP06 dependency. WP06 merges before WP05. WP02-M2 waits on WP06.
