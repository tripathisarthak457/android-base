import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Every `:data:*` module: one business domain end to end — its DTOs, its mappers, its API service
 * and its repository — depending only on `:core:*`.
 *
 * Deliberately absent: Compose, and any other `:data:*` module. A data module that needs a
 * sibling's model is a sign the domain boundary is in the wrong place, not that the graph needs
 * another edge. Keeping Compose out is also why these modules compile in parallel with the
 * feature modules above them rather than behind them.
 */
class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.base.app.android.library")
        pluginManager.apply("com.base.app.android.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("api", project(":core:common"))
            add("api", project(":core:model"))
            add("implementation", project(":core:coroutines"))
            add("implementation", project(":core:datastore"))
            // <opt:network>
            add("implementation", project(":core:network"))
            // </opt:network>

            add("implementation", libs.library("kotlinx-serialization-json"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
