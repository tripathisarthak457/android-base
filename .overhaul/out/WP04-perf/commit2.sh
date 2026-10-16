#!/usr/bin/env bash
# Round-1 fixes tested in the second round.
set -euo pipefail
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP04-perf
T=templates/android
git add $T/core/navigation/src/main/kotlin/com/base/app/core/navigation/shell/tabs/KeptTabs.kt
git commit -q -F - <<'MSG'
Lower a hidden tab's lifecycle the frame after it is hidden

Raising the incoming tab already waits two frames so its collectors restart
outside the switch frame; lowering the outgoing one still ran in that frame,
stopping every lifecycle-bound collector it had while the frame composed the
switch at the CPU's idle clock. The tab is already invisible and its Back
dispatcher is already off in that frame, so a frame's delay changes nothing
the user can see.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
MSG
git add $T/feature/sample/src/main/kotlin/com/base/app/feature/sample/detail/SampleDetailContract.kt \
        $T/feature/sample/src/main/kotlin/com/base/app/feature/sample/detail/SampleDetailViewModel.kt \
        $T/feature/sample/src/main/kotlin/com/base/app/feature/sample/detail/SampleDetailRoute.kt \
        $T/feature/sample/src/test/kotlin/com/base/app/feature/sample/detail/SampleDetailViewModelTest.kt
git commit -q -F - <<'MSG'
Compose the sample detail's related rows after the push settles

The related cards arrived from the cache 220 ms into every push and were
composed (185 composables) in a frame of the slide. The shell already tells
a screen when its host has stopped moving (LocalNavigationSettled); the
route now waits for that before asking for them, which is the rule the
reference feature is meant to demonstrate.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
MSG
git log --oneline -3
