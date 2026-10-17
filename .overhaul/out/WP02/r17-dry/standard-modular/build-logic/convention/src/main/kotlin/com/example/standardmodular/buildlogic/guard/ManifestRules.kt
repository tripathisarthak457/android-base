package com.example.standardmodular.buildlogic.guard

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/**
 * Registers `verifyManifestRules`, which reads every variant's merged manifest, libraries' entries
 * included, and fails on what stops the app adapting to its window or taking Back the modern way.
 * Wired into `check`.
 */
internal fun Project.verifyManifestRules(components: ApplicationAndroidComponentsExtension) {
    val allowList = rootProject.layout.projectDirectory.file(ManifestRules.ALLOW_FILE)
    val all = tasks.register("verifyManifestRules") {
        group = "verification"
        description = "Fails on orientation or resize locks, missing configChanges, exported or back flags."
    }

    components.onVariants { variant ->
        val task = tasks.register<VerifyManifestRulesTask>("verify${variant.name.replaceFirstChar(Char::uppercase)}ManifestRules") {
            group = "verification"
            description = "Checks the merged manifest of ${variant.name}."
            manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            if (allowList.asFile.exists()) allowed.set(allowList)
            marker.set(layout.buildDirectory.file("guards/${variant.name}ManifestRules.ok"))
        }
        all.configure { dependsOn(task) }
    }
    tasks.named { it == "check" }.configureEach { dependsOn(all) }
}

@CacheableTask
internal abstract class VerifyManifestRulesTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifest: RegularFileProperty

    /** Library components allowed to break a rule, `config/manifest-allow.txt`; absent when none are. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val allowed: RegularFileProperty

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val allowList = ManifestRules.allowList(allowed.asFile.orNull?.readText().orEmpty())
        val problems = allowList.unexplained.map { "${ManifestRules.ALLOW_FILE}: '$it' needs a reason after a `#`" } +
            ManifestRules.violations(manifest.get().asFile.readText(), allowList.names)

        if (problems.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("The merged manifest breaks the window and Back rules:")
                    appendLine()
                    problems.forEach { appendLine("  $it") }
                    appendLine()
                    append(
                        "An app that locks orientation or refuses to resize is letterboxed on tablets, foldables " +
                            "and desktop windows, and Android 16 ignores the locks there anyway. A component from a " +
                            "library goes in ${ManifestRules.ALLOW_FILE}, one name per line with `# reason`.",
                    )
                },
            )
        }
        marker.get().asFile.writeText("ok")
    }
}

/** The manifest rules, apart from Gradle so they can be unit tested on a manifest string. */
internal object ManifestRules {

    const val ALLOW_FILE = "config/manifest-allow.txt"

    /** Changes the launcher activity handles itself, so resizing, folding or a keyboard never recreates it. */
    val CONFIG_CHANGES = listOf(
        "orientation", "screenSize", "smallestScreenSize", "screenLayout", "keyboard", "keyboardHidden", "navigation",
    )

    private const val ANDROID = "http://schemas.android.com/apk/res/android"
    private const val MULTI_INSTANCE = "android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI"
    private const val RESTRICTED_RESIZABILITY = "android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY"
    private val COMPONENTS = listOf("activity", "activity-alias", "service", "receiver", "provider")
    private val LOCKS = listOf("screenOrientation", "minAspectRatio", "maxAspectRatio")

    class AllowList(val names: Set<String>, val unexplained: List<String>)

    fun allowList(text: String): AllowList {
        val entries = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        return AllowList(
            names = entries.map { it.substringBefore('#').trim() }.toSet(),
            unexplained = entries.filter { it.substringAfter('#', missingDelimiterValue = "").isBlank() },
        )
    }

    fun violations(manifest: String, allowed: Set<String> = emptySet()): List<String> {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest.byteInputStream())
        val application = document.getElementsByTagName("application").elements().firstOrNull()
            ?: return listOf("the manifest has no <application>")
        val problems = mutableListOf<String>()

        if (application.android("enableOnBackInvokedCallback") != "true") {
            problems += "<application> lacks android:enableOnBackInvokedCallback=\"true\", which predictive back needs"
        }
        val properties = document.getElementsByTagName("property").elements().associateBy { it.android("name") }
        if (properties[MULTI_INSTANCE]?.android("value") != "true") {
            problems += "<application> lacks <property android:name=\"$MULTI_INSTANCE\" android:value=\"true\" />"
        }
        if (RESTRICTED_RESIZABILITY in properties) {
            problems += "$RESTRICTED_RESIZABILITY opts out of resizing, which Android 17 no longer allows"
        }

        COMPONENTS.flatMap { tag -> document.getElementsByTagName(tag).elements().map { tag to it } }
            .forEach { (tag, component) ->
                val name = component.android("name") ?: component.android("targetActivity") ?: "?"
                if (name in allowed) return@forEach
                if (component.android("exported") == null) {
                    problems += "<$tag $name> does not say whether it is exported"
                }
                if (tag == "activity" || tag == "activity-alias") {
                    LOCKS.filter { component.android(it) != null }.forEach { problems += "<$tag $name> sets android:$it" }
                    if (component.android("resizeableActivity") == "false") {
                        problems += "<$tag $name> sets android:resizeableActivity=\"false\""
                    }
                }
                if (tag == "activity" && component.isLauncher()) {
                    val handled = component.android("configChanges").orEmpty().split('|').toSet()
                    val missing = CONFIG_CHANGES - handled
                    if (missing.isNotEmpty()) {
                        problems += "<activity $name> is recreated on ${missing.joinToString()}; add them to android:configChanges"
                    }
                }
            }
        return problems
    }

    private fun Element.android(attribute: String): String? = getAttributeNS(ANDROID, attribute).takeIf { hasAttributeNS(ANDROID, attribute) }

    private fun Element.isLauncher(): Boolean =
        getElementsByTagName("intent-filter").elements().any { filter ->
            filter.getElementsByTagName("action").elements().any { it.android("name") == "android.intent.action.MAIN" } &&
                filter.getElementsByTagName("category").elements().any { it.android("name") == "android.intent.category.LAUNCHER" }
        }

    private fun NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }
}
