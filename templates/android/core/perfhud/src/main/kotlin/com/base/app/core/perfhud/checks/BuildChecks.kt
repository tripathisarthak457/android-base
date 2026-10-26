package com.base.app.core.perfhud.checks

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.profileinstaller.ProfileVerifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.Serializable

/**
 * The facts about this build that decide how fast it can be at all, whatever its code does:
 * whether R8 ran, whether ART compiled it with the baseline profile, and whether it is debuggable,
 * which makes ART interpret most of it.
 *
 * [read] reads the profile's state from disk: off the main thread.
 */
internal class BuildChecks @Inject constructor(@ApplicationContext private val context: Context) {

    fun read(): BuildFacts = BuildFacts(
        debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        // R8 renames every class nothing keeps by name, and nothing keeps this one.
        minified = !MinifyProbe::class.java.name.endsWith(MINIFY_PROBE),
        profile = profileStatus(),
    )

    private fun profileStatus(): ProfileStatus {
        val status = ProfileVerifier.writeProfileVerification(context)
        return when {
            status.isCompiledWithProfile -> ProfileStatus.Compiled
            status.hasProfileEnqueuedForCompilation() -> ProfileStatus.Enqueued
            !status.appApkHasEmbeddedProfile() -> ProfileStatus.NotInApk
            else -> ProfileStatus.NotCompiled
        }
    }

    private class MinifyProbe

    private companion object {
        const val MINIFY_PROBE = "\$MinifyProbe"
    }
}

@Serializable
internal enum class ProfileStatus {
    /** ART compiled the app with the profile: the start-up and first-use code is ahead-of-time. */
    Compiled,

    /** Installed, waiting for the next background dexopt; `adb shell cmd package compile` forces it. */
    Enqueued,

    /** The APK has a profile that ART has not used: a debuggable build, or one installed without it. */
    NotCompiled,

    /** The APK carries no profile. */
    NotInApk,

    /** The project has no baseline profile set up. */
    NotInProject,
}

@Serializable
internal data class BuildFacts(
    val debuggable: Boolean,
    val minified: Boolean,
    val profile: ProfileStatus,
)
