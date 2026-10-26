package com.example.cmp.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * A destination belongs to the feature its key's package names (`<pkg>.feature.<owner>.api`), and
 * only that feature draws it. A key another feature opens has its file in `:core:navigation` under
 * the owner's package, where any feature can see it; this keeps such a key from being registered by
 * the feature that opens it, or twice, which would only fail when the app starts.
 *
 * Read from the sources, so it holds in either project shape, wherever the key's file lives.
 */
class SharedDestinationsTest {

    private val sources = listOf("../feature", "../core/navigation")
        .map(::File)
        .filter(File::isDirectory)
        .flatMap { root ->
            root.walk().filter {
                it.isFile && it.extension == "kt" &&
                    "/src/main/" in it.invariantSeparatorsPath
            }
        }
        .map { it to it.readText() }

    /** Every navigation key declared in a feature's api package, with the feature that owns it. */
    private val owners: Map<String, String> = sources.flatMap { (_, text) ->
        val owner = API_PACKAGE.find(text)?.groupValues?.get(1) ?: return@flatMap emptyList()
        KEY_DECLARATION.findAll(text).map { it.groupValues[1] to owner }.toList()
    }.toMap()

    /** Every `entry<Key>` registration, with the feature whose code makes it. */
    private val registrations: List<Pair<String, String>> = sources.flatMap { (_, text) ->
        val feature = FEATURE_PACKAGE.find(text)?.groupValues?.get(1) ?: return@flatMap emptyList()
        ENTRY.findAll(text).map { it.groupValues[1] to feature }.toList()
    }

    @Test
    fun `the scan sees the features' keys and graphs`() {
        // Without UI components a project can start with no feature module; the first one
        // brings its keys, and from then on the scan must see them. A module is what the
        // settings include, not a folder: a removed one can leave build output in feature/.
        val settings = File("../settings.gradle.kts").readText()
        assumeTrue("No feature module yet", FEATURE_INCLUDE.containsMatchIn(settings))
        // A source tree the scan cannot read would pass the next test with nothing checked.
        assertTrue("No keys found in any feature's api package", owners.isNotEmpty())
        assertTrue("No entry<…> registrations found", registrations.isNotEmpty())
    }

    @Test
    fun `every feature key is registered once, by the feature that owns it`() {
        val problems = owners.mapNotNull { (key, owner) ->
            val by = registrations.filter { it.first == key }.map { it.second }
            when {
                by.isEmpty() -> "$key (owned by $owner) is registered by no feature"
                by.size > 1 -> "$key is registered ${by.size} times, by $by"
                by.single() != owner -> "$key is owned by $owner but registered by ${by.single()}"
                else -> null
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private companion object {
        val FEATURE_PACKAGE = Regex("""^package [\w.]+\.feature\.(\w+)""", RegexOption.MULTILINE)
        val API_PACKAGE = Regex("""^package [\w.]+\.feature\.(\w+)\.api\b""", RegexOption.MULTILINE)

        // The supertype after the name or the constructor's parameters, so a parameter typed AppNavKey
        // does not make its class a key.
        val KEY_DECLARATION =
            Regex(
                """(?:object|class)\s+(\w+)(?:\s*\([^)]*\))?\s*:\s*(?:AppNavKey|FullScreenKey)\b""",
            )
        val ENTRY = Regex("""\bentry<\s*(\w+)\s*>""")
        val FEATURE_INCLUDE = Regex("""^\s*include\(":feature:""", RegexOption.MULTILINE)
    }
}
