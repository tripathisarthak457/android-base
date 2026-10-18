import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.base.app.buildlogic.BundleLicensesTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/** Puts the app's real dependency licences into its assets, per variant. */
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
