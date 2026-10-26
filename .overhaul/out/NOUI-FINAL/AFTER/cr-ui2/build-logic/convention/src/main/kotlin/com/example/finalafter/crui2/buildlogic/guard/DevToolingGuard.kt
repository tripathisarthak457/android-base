package com.example.finalafter.crui2.buildlogic.guard

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.example.finalafter.crui2.buildlogic.config.AppFlavor
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

/**
 * Fails the build of a release-like variant outside the dev flavour, and `check`, when it would
 * carry dev tooling. Release-like is every build type but debug: release, and the benchmarkRelease
 * and nonMinifiedRelease the baseline profile plugin adds, which measure what ships.
 *
 * :app wires those modules with `debugImplementation` and `devImplementation` only, and the root
 * draws whatever tooling is bound. One `implementation(...)`, in :app or in any module a shippable
 * build has, would put the inspector in front of every user of the production app, with nothing at
 * run time to stop it.
 */
internal fun Project.verifyReleasesCarryNoDevTooling(components: ApplicationAndroidComponentsExtension) {
    components.onVariants { variant ->
        if (variant.buildType == DEBUG_BUILD_TYPE) return@onVariants
        if (variant.productFlavors.any { it.second == AppFlavor.DEV.flavorName }) return@onVariants

        val capitalised = variant.name.replaceFirstChar { it.uppercase() }
        val task = tasks.register<VerifyNoDevToolingTask>("verify${capitalised}CarriesNoDevTooling") {
            group = "verification"
            description = "Fails if ${variant.name} depends on a dev tooling module."
            variantName.set(variant.name)
            dependencyGraph.set(variant.runtimeConfiguration.incoming.resolutionResult.rootComponent)
            marker.set(layout.buildDirectory.file("guards/$name.ok"))
        }
        val hooks = setOf("check", "pre${capitalised}Build")
        tasks.named { it in hooks }.configureEach { dependsOn(task) }
    }
}

@DisableCachingByDefault(because = "A walk of the resolved graph; a cached marker is no faster.")
internal abstract class VerifyNoDevToolingTask : DefaultTask() {

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val dependencyGraph: Property<ResolvedComponentResult>

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val found = devToolingIn(dependencyGraph.get())
        if (found.isNotEmpty()) {
            throw GradleException(
                "${variantName.get()} depends on ${found.joinToString()}. Dev tooling reaches dev-flavour and " +
                    "debug builds only: depend on it with debugImplementation and \"devImplementation\" from " +
                    ":app, never with implementation or api, and never from a module every build has.",
            )
        }
        marker.get().asFile.writeText("ok")
    }
}

/** Every dev tooling module anywhere in [root]'s graph, directly or through another module. */
private fun devToolingIn(root: ResolvedComponentResult): List<String> {
    val seen = HashSet<ComponentIdentifier>()
    val pending = ArrayDeque(listOf(root))
    val found = sortedSetOf<String>()
    while (pending.isNotEmpty()) {
        val component = pending.removeFirst()
        if (!seen.add(component.id)) continue
        val id = component.id
        if (id is ProjectComponentIdentifier && id.projectPath in DEV_TOOLING_MODULES) found += id.projectPath
        component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { pending += it.selected }
    }
    return found.toList()
}

private const val DEBUG_BUILD_TYPE = "debug"

private val DEV_TOOLING_MODULES = setOf(
    ":core:devtools",
    ":core:perfhud",
)
