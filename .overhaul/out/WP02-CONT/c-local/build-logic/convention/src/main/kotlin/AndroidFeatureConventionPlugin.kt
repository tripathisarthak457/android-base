import io.example.fitclocal.buildlogic.library
import io.example.fitclocal.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Every `:feature:*` module: presentation, and nothing else. Only what every screen uses is added
 * here; a feature that needs images, paging or a data module declares it itself.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.example.fitclocal.android.library")
        pluginManager.apply("io.example.fitclocal.android.hilt")
        pluginManager.apply("io.example.fitclocal.android.compose")
        // Every screen's previews double as its screenshot tests, at every window size.
        pluginManager.apply("io.example.fitclocal.android.screenshots")
        // Navigation keys are @Serializable, so the back stack survives process death.
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:mvi"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:navigation"))

            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
        }
    }
}
