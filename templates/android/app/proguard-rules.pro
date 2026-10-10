# R8 rules for the application.
#
# Most libraries here ship their own consumer rules — Ktor, Room, Hilt and kotlinx-serialization
# all do — so this file is deliberately short. Every rule below exists because something in this
# project would otherwise break in a release build and *only* in a release build, which is the
# most expensive class of bug there is.

# ── Crash reports that can be read ──────────────────────────────────────────
# Without these, a stack trace has no line numbers and every frame reads as `Unknown Source`.
# SourceFile is renamed rather than kept, so the mapping file is still required to deobfuscate.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Coroutines ──────────────────────────────────────────────────────────────
# The debug agent's probes are development-only and pull in a chunk of reflection.
-dontwarn kotlinx.coroutines.debug.**

# ── OkHttp / Ktor engine ────────────────────────────────────────────────────
# Optional platform integrations OkHttp references reflectively and that are absent on Android.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# <opt:playstore>
# Play's review-ktx references an annotation that ships in no artifact it depends on, so R8 stops
# on a missing class rather than a missing rule. Debug builds do not minify, so the first place
# this appears is a release build — which is to say, the day of a release.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
# </opt:playstore>
