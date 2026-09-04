package com.base.app.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Collects every release artifact into `build/outputs/dist/<variant>/` under a name that says
 * what it is, and writes a checksum manifest beside them.
 *
 * The default AGP output is four files all called `app-release.apk` in four sibling directories.
 * The moment two of them are attached to a bug report or uploaded to a distribution service,
 * nobody can tell the arm64 staging build from the universal production one. A name that carries
 * the variant, the ABI, the version and the minute it was built removes that whole class of
 * question.
 *
 * Renaming happens by copy rather than by setting `outputFileName`, which is only reachable
 * through AGP internals. The copy is cheap, keeps the original tree intact for anything that
 * expects it, and gives one directory to hand to CI as the upload set.
 */
internal fun Project.registerDistributionTasks(components: ApplicationAndroidComponentsExtension) {
    val distAll = tasks.register("dist") {
        group = "distribution"
        description = "Copies every assembled release artifact into build/outputs/dist, named and checksummed."
    }

    components.onVariants { variant ->
        if (variant.buildType != "release") return@onVariants

        val capitalised = variant.name.replaceFirstChar { it.uppercase() }

        val apkTask = tasks.register<DistributeApksTask>("dist${capitalised}Apks") {
            group = "distribution"
            description = "Names and checksums the ${variant.name} APKs into build/outputs/dist."
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            appName.set(AppConfig.APP_NAME)
            variantName.set(variant.name)
            outputDirectory.set(layout.buildDirectory.dir("outputs/dist/${variant.name}"))
        }

        val bundleTask = tasks.register<DistributeBundleTask>("dist${capitalised}Bundle") {
            group = "distribution"
            description = "Names and checksums the ${variant.name} app bundle into build/outputs/dist."
            bundleFile.set(variant.artifacts.get(SingleArtifact.BUNDLE))
            appName.set(AppConfig.APP_NAME)
            variantName.set(variant.name)
            outputDirectory.set(layout.buildDirectory.dir("outputs/dist/${variant.name}"))
        }

        // The one people actually type. It covers the APKs only, and that is not an oversight:
        // AGP refuses to build ABI splits and an app bundle in the same invocation, because the
        // shrunk-resources output would be ambiguous. The two jobs are also genuinely different —
        // per-ABI APKs are for handing to testers, a bundle is for Play, which does its own
        // splitting — so `dist<Variant>Bundle` stays a separate call.
        val variantTask = tasks.register("dist$capitalised") {
            group = "distribution"
            description = "Names and checksums the ${variant.name} APKs. Bundle: dist${capitalised}Bundle."
            dependsOn(apkTask)
        }

        distAll.configure { dependsOn(variantTask) }
    }
}

private const val TIMESTAMP_PATTERN = "yyyyMMdd-HHmm"

private fun timestamp(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN))

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { stream ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun writeManifest(directory: File, entries: List<Pair<File, String>>) {
    val manifest = File(directory, "checksums.sha256")
    manifest.writeText(
        entries.joinToString(separator = "\n", postfix = "\n") { (file, hash) ->
            "$hash  ${file.name}  ${file.length()} bytes"
        },
    )
}

internal abstract class DistributeApksTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun distribute() {
        val artifacts = builtArtifactsLoader.get().load(apkDirectory.get())
            ?: throw GradleException("No APKs found for ${variantName.get()}.")

        val destination = outputDirectory.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }
        val stamp = timestamp()

        val copied = artifacts.elements.map { element ->
            // A split carries an ABI filter; the universal APK carries none, which is exactly how
            // it is told apart from the others.
            val abi = element.filters
                .firstOrNull { it.filterType.name == "ABI" }
                ?.identifier
                ?: "universal"

            val name = buildString {
                append(appName.get())
                append('-').append(variantName.get())
                append('-').append(abi)
                append('-').append(AppConfig.VERSION_NAME)
                append('-').append(AppConfig.VERSION_CODE)
                append('-').append(stamp)
                append(".apk")
            }

            val source = File(element.outputFile)
            val target = File(destination, name)
            source.copyTo(target, overwrite = true)
            target to target.sha256()
        }

        writeManifest(destination, copied)
        logger.lifecycle("[dist] ${copied.size} APK(s) → ${destination.absolutePath}")
    }
}

internal abstract class DistributeBundleTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundleFile: RegularFileProperty

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun distribute() {
        val destination = outputDirectory.get().asFile.apply { mkdirs() }
        val name = buildString {
            append(appName.get())
            append('-').append(variantName.get())
            append('-').append(AppConfig.VERSION_NAME)
            append('-').append(AppConfig.VERSION_CODE)
            append('-').append(timestamp())
            append(".aab")
        }

        val target = File(destination, name)
        bundleFile.get().asFile.copyTo(target, overwrite = true)

        val existing = File(destination, "checksums.sha256").takeIf { it.exists() }?.readText().orEmpty()
        File(destination, "checksums.sha256").writeText(
            existing + "${target.sha256()}  ${target.name}  ${target.length()} bytes\n",
        )
        logger.lifecycle("[dist] bundle → ${target.absolutePath}")
    }
}
