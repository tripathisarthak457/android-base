import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Firebase, applied by the application module only.
 *
 * The `google-services` plugin resolves `google-services.json` per flavour, looking first in
 * `src/<flavour>/` and falling back to `app/`. Each environment therefore gets its own Firebase
 * project without any build-file branching — see `app/src/dev/google-services.json`.
 */
class AndroidFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.gms.google-services")
        pluginManager.apply("com.google.firebase.crashlytics")

        dependencies {
            add("implementation", platform(libs.library("firebase-bom")))
            add("implementation", libs.library("firebase-analytics"))
            add("implementation", libs.library("firebase-crashlytics"))
            // <opt:push>
            add("implementation", libs.library("firebase-messaging"))
            // </opt:push>
        }
    }
}
