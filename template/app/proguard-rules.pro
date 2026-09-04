# R8 rules for the application.
#
# Most libraries here ship their own consumer rules — Ktor, Room, Hilt and kotlinx-serialization
# all do — so this file is deliberately short. Every rule below exists because something in this
# project would otherwise break in a release build and *only* in a release build, which is the
# most expensive class of bug there is.

# ── kotlinx.serialization ────────────────────────────────────────────────────
# The library's own rules cover @Serializable classes reached statically. They do not cover the
# polymorphic navigation keys: those are resolved by class name at runtime, from a JSON string in
# a saved-state bundle, so R8 sees nothing referencing them and renames them. The symptom is
# an app that restores the wrong screen after being killed in the background — release only.
-keepclassmembers class ** implements com.base.app.core.navigation.AppNavKey {
    *** Companion;
}
-keep,includedescriptorclasses class ** implements com.base.app.core.navigation.AppNavKey {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Data transfer objects ───────────────────────────────────────────────────
# Serializer lookup for a generic type argument goes through reflection on the type parameter,
# which R8 cannot follow. Keeping the generated serializers is enough; the classes themselves can
# still be shrunk and renamed.
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Enum entries ────────────────────────────────────────────────────────────
# `enumValues`/`valueOf` are used by the settings store to round-trip an enum through a string.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

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
