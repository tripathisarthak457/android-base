package com.base.app.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Copies licensee's report into the variant's assets under a name the app reads at runtime.
 *
 * A task rather than a `Copy` because AGP's `addGeneratedSourceDirectory` needs a task with a
 * `DirectoryProperty` output it can point the asset merger at — that is what makes the generated
 * file a first-class input to the build rather than something written into a directory and hoped
 * for.
 */
abstract class BundleLicensesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reportFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun bundle() {
        // `asFileTree` rather than `files`: licensee declares a directory as its output, and
        // a file collection holding a directory yields the directory itself, not what is in it.
        val report = reportFiles.asFileTree.files.firstOrNull { it.name == REPORT_NAME }
            ?: throw GradleException(
                "licensee produced no $REPORT_NAME. Check that app.cash.licensee is applied and " +
                    "that its own task ran.",
            )

        outputDirectory.file(ASSET_NAME).get().asFile.writeText(report.readText())
    }

    private companion object {
        const val REPORT_NAME = "artifacts.json"
        const val ASSET_NAME = "licenses.json"
    }
}
