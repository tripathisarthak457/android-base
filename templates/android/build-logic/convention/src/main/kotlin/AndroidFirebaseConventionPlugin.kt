import org.gradle.api.Plugin
import org.gradle.api.Project
// <opt:crashlytics>
import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
// </opt:crashlytics>

/**
 * Firebase for the application module: the google-services plugin, which reads
 * `google-services.json`, and Crashlytics' plugin when crash reporting is on. Each SDK is a
 * dependency of the module that uses it, so this adds none.
 */
class AndroidFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.gms.google-services")
        // <opt:crashlytics>
        pluginManager.apply("com.google.firebase.crashlytics")

        // Uploading the R8 mapping needs a real Firebase project and a network, which local and CI
        // builds have no business needing: the release pipeline passes -PcrashlyticsUpload=true.
        val upload = providers.gradleProperty("crashlyticsUpload").map(String::toBoolean).getOrElse(false)
        extensions.configure<ApplicationExtension> {
            buildTypes.configureEach {
                (this as ExtensionAware).extensions.configure<CrashlyticsExtension> {
                    mappingFileUploadEnabled = upload
                }
            }
        }
        // </opt:crashlytics>
    }
}
