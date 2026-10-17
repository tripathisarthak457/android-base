import com.example.none.buildlogic.library
import com.example.none.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Firebase, applied by the application module only. */
class AndroidFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.gms.google-services")
        pluginManager.apply("com.google.firebase.crashlytics")

        dependencies {
            add("implementation", platform(libs.library("firebase-bom")))
            add("implementation", libs.library("firebase-analytics"))
            add("implementation", libs.library("firebase-crashlytics"))
        }
    }
}
