package com.base.app.core.ui.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app's own language, independent of the phone's.
 *
 * On Android 13 and later this is the system's per-app language: it shows up in the phone's
 * settings too, and the system restarts the activity itself. Earlier releases have no such
 * thing, so the choice is kept here and applied by wrapping the activity's context — which is
 * why `MainActivity.attachBaseContext` calls [wrap].
 *
 * Deliberately not AppCompat's `setApplicationLocales`. Below 13 that only works in an
 * `AppCompatActivity`, and this app has no other reason to depend on AppCompat.
 *
 * Add a language by adding `values-xx` directories and putting its tag in [supported]. The
 * phone's settings pick it up by themselves: the build generates their list from the same
 * directories.
 */
object AppLocales {

    /** Every language the app ships strings for, source language first. */
    val supported: List<String> = listOf("en")

    /** The chosen language tag, or null when the app follows the phone. */
    fun current(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeUnless { it.isEmpty }
                ?.toLanguageTags()
        } else {
            prefs(context).getString(KEY, null)
        }

    /** Applies [tag], or follows the phone again when it is null. */
    fun set(context: Context, tag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            prefs(context).edit().apply { if (tag == null) remove(KEY) else putString(KEY, tag) }.apply()
            (context as? Activity)?.recreate()
        }
    }

    /** For `attachBaseContext` below Android 13. Returns [base] unchanged everywhere else. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY, null) ?: return base
        val configuration = base.resources.configuration.apply {
            setLocales(LocaleList.forLanguageTags(tag))
        }
        return base.createConfigurationContext(configuration)
    }

    /** "Français" for `fr`, written in its own language — which is how people look for it. */
    fun displayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    // SharedPreferences, not DataStore: attachBaseContext runs before anything could await a
    // flow, and this is one small string read once per activity.
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private const val FILE = "app_locale"
    private const val KEY = "tag"
}
