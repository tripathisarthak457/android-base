"""instrument.py <tree>: the back-gesture experiment's trace counters (DESIGN.md §5.2), inserted into one extracted
template tree (out/WP04-perf/s/bg-*), never into a worktree. The same edits go into every build of the ladder, by
anchors that none of the ladder's patches change, so head, tex, c1 and c2 carry identical instrumentation:

  BackCard.write  raw progress x1000, as BackCardState.track receives it
  BackCard.apply  the pose x1000 and BackCard.alpha the content alpha x1000, each time applyCard runs (the layer's
                  properties are set: what the next traversal draws)
  BackCard.phase  1 onBackStarted, 2 onBackCompleted, 3 onBackCancelled, 0 the card reset
  BackCard.warm   1 / 0 as c2's prewarm takes and drops the texture (only where c2's warm()/cool() exist)

plus wildcard baseline-profile rules for the touched classes, so every build compiles them ahead of time alike.
Exits non-zero if an anchor is missing, so a build never runs half-instrumented."""
import pathlib, re, sys

root = pathlib.Path(sys.argv[1]) / "templates/android/core/navigation/src/main"
nav = root / "kotlin/com/base/app/core/navigation"
back = nav / "back"

def edit(path, pairs):
    text = path.read_text(encoding="utf-8")
    for pattern, replacement, required in pairs:
        new, n = re.subn(pattern, replacement, text, count=1, flags=re.M)
        if n == 0 and required:
            sys.exit(f"instrument.py: anchor not found in {path.name}: {pattern}")
        text = new
    path.write_text(text, encoding="utf-8", newline="\n")

(back / "BackTrace.kt").write_text("""package com.base.app.core.navigation.back

import android.os.Build
import android.os.Trace
import kotlin.math.roundToLong

/** Experiment-only trace counters (research/back-google/DESIGN.md); never committed. */
internal object BackTrace {
    fun write(progress: Float) = counter("BackCard.write", progress)

    fun apply(pose: Float, alpha: Float) {
        counter("BackCard.apply", pose)
        counter("BackCard.alpha", alpha)
    }

    fun phase(value: Int) = counter("BackCard.phase", value / 1000f)

    fun warm(on: Boolean) = counter("BackCard.warm", if (on) 0.001f else 0f)

    private fun counter(name: String, value: Float) {
        if (Build.VERSION.SDK_INT >= 29) Trace.setCounter(name, (value * 1000f).roundToLong())
    }
}
""", encoding="utf-8", newline="\n")

edit(back / "BackCard.kt", [
    (r"^([ \t]*)fun track\(event: NavigationEvent\) \{\n", r"\g<0>\1    BackTrace.write(event.progress)\n", True),
    (r"^([ \t]*)fun GraphicsLayerScope\.applyCard\(\) \{\n", r"\g<0>\1    BackTrace.apply(pose, contentAlpha.value)\n", True),
    (r"^([ \t]*)private suspend fun reset\(\) \{\n", r"\g<0>\1    BackTrace.phase(0)\n", True),
    (r"^([ \t]*)fun warm\(subject: Any\) \{\n", r"\g<0>\1    BackTrace.warm(true)\n", False),
    (r"^([ \t]*)fun cool\(\) \{\n", r"\g<0>\1    BackTrace.warm(false)\n", False),
])
edit(back / "StackBackHandler.kt", [
    (r"^([ \t]*)override fun onBackStarted\(event: NavigationEvent\) = follow\(event\)\n",
     r"\1override fun onBackStarted(event: NavigationEvent) {\n\1    BackTrace.phase(1)\n\1    follow(event)\n\1}\n", True),
    (r"^([ \t]*)override fun onBackCompleted\(\) \{\n", r"\g<0>\1    BackTrace.phase(2)\n", True),
    (r"^([ \t]*)override fun onBackCancelled\(\) \{\n", r"\g<0>\1    BackTrace.phase(3)\n", True),
])
prof = root / "baseline-prof.txt"
prof.write_text(prof.read_text(encoding="utf-8").rstrip("\n") + "\n" + "\n".join([
    "HSPLcom/base/app/core/navigation/back/**;->**(**)**",
    "HSPLcom/base/app/core/navigation/stack/StackScene;->**(**)**",
    "HSPLcom/base/app/core/navigation/stack/StackScene**;->**(**)**",
    "HSPLcom/base/app/core/navigation/stack/StackHost**;->**(**)**",
]) + "\n", encoding="utf-8", newline="\n")
print(f"INSTRUMENTED {sys.argv[1]}")
