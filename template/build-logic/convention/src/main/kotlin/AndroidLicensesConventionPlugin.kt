import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.base.app.buildlogic.BundleLicensesTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Puts the app's real dependency licences into its assets, per variant.
 *
 * The list is generated from the resolved runtime classpath at build time rather than written by
 * hand, because a hand-written one is out of date the first time anybody adds a library — and it
 * is the kind of stale that nobody notices until a store review asks about it.
 *
 * Applied *after* `app.cash.licensee` in the same plugins block; it reads that plugin's report
 * rather than resolving anything itself. Licensee also fails the build on a licence the project
 * has not allowed, which is how a GPL dependency becomes a build error rather than a discovery.
 */
class AndroidLicensesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        if (!pluginManager.hasPlugin("app.cash.licensee")) {
            throw GradleException(
                "com.base.app.android.licenses needs app.cash.licensee applied first — add " +
                    "alias(libs.plugins.licensee) above it in the plugins block.",
            )
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val capitalised = variant.name.replaceFirstChar(Char::uppercase)
                val report = tasks.named("licenseeAndroid$capitalised")

                val bundle = tasks.register<BundleLicensesTask>("bundle${capitalised}Licenses") {
                    // Depends on the report through its outputs rather than by path, so the two
                    // stay wired if licensee ever moves where it writes.
                    reportFiles.setFrom(report.map { it.outputs.files })
                }

                variant.sources.assets?.addGeneratedSourceDirectory(
                    bundle,
                    BundleLicensesTask::outputDirectory,
                )
            }
        }
    }
}
