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

/** Compose, minus Material. The dependency set is foundation + ui + animation and nothing else. */
internal fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Marks java.time and similar types immutable so composables taking them can skip.
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
    // Enables the metrics report and the task that reads it; see ComposeStability.kt.
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

/** Three build-time guards over every module's Kotlin sources. */
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

    /** Copy typed straight into a composable, where a resource should be. */
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

        // Plain strings, not raw ones: a regex ending in a quote inside """ is hard to read.
        val FUNCTION = Regex("\\bfun\\s+(\\w+)\\s*\\(")
        val COPY_PARAMETER = Regex(
            "\\b(text|title|label|helper|supporting|message|description|placeholder|" +
                "overline|subtitle|actionLabel|caption|contentDescription)\\s*=\\s*\"([^\"]*)\"",
        )
    }
}
