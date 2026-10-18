import com.example.hud.standard.buildlogic.libs
import com.example.hud.standard.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** Every `:feature:*` module: presentation, and nothing else. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.example.hud.standard.android.library")
        pluginManager.apply("com.example.hud.standard.android.hilt")
        pluginManager.apply("com.example.hud.standard.android.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:common"))
            add("implementation", project(":core:mvi"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:navigation"))

            add("implementation", libs.library("kotlinx-serialization-json"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("coil-compose"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
