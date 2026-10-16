#!/usr/bin/env bash
# The round-1 fixes as four commits, each staged by path.
set -euo pipefail
cd /home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP04-perf
T=templates/android
git add $T/core/navigation/src/main/kotlin/com/base/app/core/navigation/stack/StackLayout.kt \
        $T/core/navigation/src/test/kotlin/com/base/app/core/navigation/recomposition/ShellRecompositionTest.kt
git commit -q -F - <<'MSG'
Keep a leaving screen's pane context through its pop

Once an entry was popped it belonged to no page, so the layout gave it the
default pane context, and LocalPaneContext is a static local: the new value
composed the whole leaving screen again (416 composable executions for the
sample detail) in the first frame of the pop, the frame that starts the slide
with the CPU still at its idle clock. The entry now keeps the context it had
until it has slid off.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
MSG
git add $T/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/navigation/AppTabBar.kt
git commit -q -F - <<'MSG'
Read the bar's selection through state in its tap handler

The handler captured the selected index, so every switch made a new lambda,
every cell's selectable modifier changed with it and all ten cells of the
bar recomposed in the switch's first frame. Read through state, the handler
is the same object for the life of the bar and only the two cells whose
selection flips recompose.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
MSG
git add $T/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/navigation/TabCells.kt
git commit -q -F - <<'MSG'
Build the selection pill as a rounded rect, not cubics

A path of lines and cubic arcs is a general path to Skia, so the two cell
layers clipped to it were masked on every frame the pill moved, and the fill
went through the path renderer. The same capsule added as a rounded rect is
recognised as one and clipped and filled analytically. The RoundRect it
allocates per frame is three small objects; the mask it replaces was a pass
over the bar.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
MSG
git add $T/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/layout/Chrome.kt \
        $T/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/container/AppScaffold.kt \
        $T/core/navigation/src/main/kotlin/com/base/app/core/navigation/stack/EntryFrame.kt \
        $T/core/navigation/src/main/kotlin/com/base/app/core/navigation/presentation/PresentationHost.kt
git commit -q -F - <<'MSG'
Paint a screen's background once per frame

Every screen in a stack was filled twice on every frame: once by its entry's
frame, which must be opaque while two screens slide over each other, and
again by AppScaffold's default background. A full-window fill is 2.6 Mpx, a
third of the GPU time of a frame on the Redmi, and the first frame of a
transition runs with the GPU at its idle clock. The frame now tells the
screen what it has painted (LocalPaintedBackground), and a scaffold on such
a screen paints nothing of its own unless given a different colour.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
MSG
git log --oneline -6
