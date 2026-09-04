import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Every `:feature:*` module: presentation, and nothing else.
 *
 * A feature owns its MVI contract, its ViewModel, its composables and its navigation keys. It
 * reaches data exclusively through the `:data:*` module it declares for itself, and it never
 * depends on another feature — cross-feature state travels through a singleton exposed by a data
 * module, which is what keeps the graph acyclic by construction rather than by review. The
 * `moduleGraphCheck` task fails the build if that rule is broken.
 *
 * Note what is absent: any navigation library. Features implement `AppNavKey` from
 * `:core:navigation` and register their destinations through Hilt, so the day Navigation 3 is
 * replaced, no feature module changes.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.base.app.android.library")
        pluginManager.apply("com.base.app.android.hilt")
        pluginManager.apply("com.base.app.android.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:common"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:navigation"))
            add("implementation", project(":core:coroutines"))

            add("implementation", libs.library("kotlinx-serialization-json"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-hilt-navigation-compose"))
            // <opt:coil>
            add("implementation", libs.library("coil-compose"))
            // </opt:coil>

            add("testImplementation", project(":core:testing"))
        }
    }
}
