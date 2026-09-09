package com.base.app.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Compose, minus Material.
 *
 * The dependency set is foundation + ui + animation and nothing else. Every visual primitive the
 * app uses comes from `:core:designsystem`, which is built on those. See [verifyNoMaterial] for
 * the guard that keeps it that way.
 */
internal fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Tells the compiler that types it cannot see inside — java.time, kotlinx.collections —
        // are in fact immutable, so composables taking them can skip recomposition instead of
        // conservatively re-running. Without this a LocalDate parameter alone is enough to make a
        // whole subtree unskippable.
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("config/compose-stability.conf"),
        )

        val metricsEnabled = providers.gradleProperty("enableComposeCompilerMetrics")
            .orNull?.toBoolean() == true
        val reportsEnabled = providers.gradleProperty("enableComposeCompilerReports")
            .orNull?.toBoolean() == true

        if (metricsEnabled) {
            metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
        }
        if (reportsEnabled) {
            reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
        }
    }

    // <opt:composemetrics>
    // Turns the metrics report on for the two modules whose components every screen calls, and
    // registers the task that reads it. Everything it needs is in ComposeStability.kt, including
    // the decision about which modules those are.
    configureComposeStability()
    // </opt:composemetrics>

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            optIn.addAll(
                "androidx.compose.foundation.ExperimentalFoundationApi",
                "androidx.compose.ui.ExperimentalComposeUiApi",
                "androidx.compose.animation.ExperimentalAnimationApi",
            )
        }
    }

    dependencies {
        val bom = libs.library("androidx-compose-bom")
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))

        add("implementation", libs.library("androidx-compose-runtime"))
        add("implementation", libs.library("androidx-compose-foundation"))
        add("implementation", libs.library("androidx-compose-ui"))
        add("implementation", libs.library("androidx-compose-ui-graphics"))
        add("implementation", libs.library("androidx-compose-animation"))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))

        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
    }
}

/**
 * Three build-time guards over every module's Kotlin sources.
 *
 * **Composable without the compiler plugin.** Compose's runtime annotations arrive transitively
 * from plenty of libraries, so a module can declare `@Composable` functions and compile perfectly
 * happily without the compiler plugin applied. Those functions are emitted as ordinary ones, with
 * no `Composer` parameters. A caller in a module that *does* have the plugin emits a call to the
 * composable signature, the two disagree, and the only symptom is a `NoSuchMethodError` the first
 * time that screen renders. Adding the plugin is one line; discovering you needed it is a crash
 * on a device.
 *
 * **A Material import.** This project's design system is a complete, self-contained set of
 * components with its own tokens. One `androidx.compose.material3` import is how that becomes two
 * competing systems: a screen picks up `MaterialTheme.colorScheme.primary`, it renders close
 * enough to the real accent that nobody notices in review, and six months later half the app
 * ignores the palette. The check is here rather than in review because it is exactly the kind of
 * line an IDE auto-import adds without anybody typing it.
 *
 * **Copy written into Kotlin.** Feature modules only. The project has string resources per
 * feature and `UiText` for what a ViewModel produces, and had neither used: every screen was
 * English in the source, so a second language meant a week of finding it. One `stringResource`
 * is easy to add and impossible to remember, so it is checked rather than asked for.
 */
internal fun Project.registerComposeGuards() {
    val kotlinSources = fileTree("src") { include("**/*.kt") }
    val projectPath = path

    val guard = tasks.register<VerifyComposeUsageTask>("verifyComposeUsage") {
        group = "verification"
        description = "Fails on Material, a @Composable without the compiler, or untranslatable copy."
        sources.setFrom(kotlinSources)
        moduleName.set(projectPath)
        // Resolved lazily: the convention plugins apply the Compose plugin after this one, so at
        // configuration time the answer would always be "no".
        composePluginApplied.set(
            provider { pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose") },
        )
        // Feature modules only, decided from the path. The design system takes its copy as
        // parameters and the catalog's copy is documentation, so neither is translated.
        localisedStrings.set(projectPath.startsWith(":feature:"))
    }

    tasks.named("preBuild") { dependsOn(guard) }
}

internal abstract class VerifyComposeUsageTask : org.gradle.api.DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: org.gradle.api.file.ConfigurableFileCollection

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val composePluginApplied: Property<Boolean>

    /** Only feature modules. The design system takes its copy as parameters. */
    @get:Input
    abstract val localisedStrings: Property<Boolean>

    @TaskAction
    fun verify() {
        val files = sources.files.filter { it.isFile }
        verifyNoMaterial(files)
        verifyComposeCompiler(files)
        if (localisedStrings.get()) verifyNoHardcodedCopy(files)
    }

    private fun verifyNoMaterial(files: List<java.io.File>) {
        val offenders = files
            .mapNotNull { file ->
                val line = file.readLines().firstOrNull { it.trimStart().startsWith(MATERIAL_IMPORT) }
                line?.let { "${file.name}: ${it.trim()}" }
            }
            .take(MAX_REPORTED)

        if (offenders.isEmpty()) return

        throw org.gradle.api.GradleException(
            """
            ${moduleName.get()} imports Compose Material. This project's design system replaces it
            entirely — see :core:designsystem, and the catalog app for what is available.

            ${offenders.joinToString("\n            ")}

            Use the equivalent from com.base.app.core.designsystem.component. If something genuinely
            has no equivalent yet, add it to the design system rather than reaching for Material in
            one screen.
            """.trimIndent(),
        )
    }

    /**
     * Copy typed straight into a composable, where a resource should be.
     *
     * The project has the machinery for this — string resources per feature module, `UiText` for
     * the strings a ViewModel produces — and it had none of the discipline: every screen was
     * English in Kotlin, and the first attempt at a second language would have been a week of
     * finding them. One `stringResource` is easy to add and impossible to remember, so it is
     * checked instead of asked for.
     *
     * Deliberately narrow. Only the named parameters that carry visible copy, only when the
     * literal reads like prose — a lowercase letter and a space — so that an identifier, a symbol
     * or a format fragment passes. Preview functions are exempt: their fake data is not shipped,
     * and making somebody translate "A sample row" teaches the wrong lesson about what this is for.
     */
    private fun verifyNoHardcodedCopy(files: List<java.io.File>) {
        val offenders = mutableListOf<String>()

        // Production sources only. A fake response in a test says "Not found" because that is
        // what the server would say, and nobody is translating it.
        for (file in files.filter { it.invariantSeparatorsPath.contains(MAIN_SOURCES) }) {
            var inPreview = false
            file.readLines().forEachIndexed { index, line ->
                val declaration = FUNCTION.find(line)
                if (declaration != null) inPreview = declaration.groupValues[1].endsWith("Preview")
                if (inPreview) return@forEachIndexed

                val match = COPY_PARAMETER.find(line) ?: return@forEachIndexed
                val literal = match.groupValues[2]
                if (!literal.contains(' ') || literal.none { it.isLowerCase() }) return@forEachIndexed
                offenders += "${file.name}:${index + 1}: ${line.trim()}"
            }
        }

        if (offenders.isEmpty()) return

        val listed = offenders.take(MAX_REPORTED).joinToString(separator = "\n            ")
        throw org.gradle.api.GradleException(
            """
            ${moduleName.get()} renders copy that is written into the Kotlin rather than into a
            string resource, so it cannot be translated and cannot be changed without a rebuild.

            $listed

            Move each one to src/main/res/values/strings.xml and read it with
            stringResource(R.string.your_key). For a string a ViewModel produces rather than a
            composable, use UiText.of(R.string.your_key) — it resolves at render time, so it
            follows a locale change without the ViewModel knowing there was one.
            """.trimIndent(),
        )
    }

    private fun verifyComposeCompiler(files: List<java.io.File>) {
        if (composePluginApplied.get()) return

        val offenders = files
            .filter { it.readText().contains("@Composable") }
            .map { it.name }
            .take(MAX_REPORTED)

        if (offenders.isEmpty()) return

        throw org.gradle.api.GradleException(
            """
            ${moduleName.get()} declares @Composable functions but does not apply the Compose
            compiler plugin, so they compile without their Composer parameters and crash with
            NoSuchMethodError the first time they are called.

            Offending files: ${offenders.joinToString()}

            Fix: add id("com.base.app.android.compose") to ${moduleName.get()}'s plugins block,
            or use com.base.app.android.feature, which includes it.
            """.trimIndent(),
        )
    }

    private companion object {
        const val MATERIAL_IMPORT = "import androidx.compose.material"
        const val MAX_REPORTED = 5
        const val MAIN_SOURCES = "/src/main/"

        // Plain strings rather than raw ones: a regex that ends in a quote and lives in a
        // """ literal is four quotes in a row, and which of them belong to the pattern is a
        // question nobody should have to answer while reading a build guard.
        val FUNCTION = Regex("\\bfun\\s+(\\w+)\\s*\\(")
        val COPY_PARAMETER = Regex(
            "\\b(text|title|label|helper|supporting|message|description|placeholder|" +
                "overline|subtitle|actionLabel|caption|contentDescription)\\s*=\\s*\"([^\"]*)\"",
        )
    }
}
