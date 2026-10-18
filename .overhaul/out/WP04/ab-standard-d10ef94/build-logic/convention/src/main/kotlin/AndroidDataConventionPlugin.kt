import com.example.wp04.abstd.buildlogic.libs
import com.example.wp04.abstd.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Every `:data:*` module: one business domain end to end — its DTOs, its mappers, its API service
 * and its repository — depending only on `:core:*`.
 */
class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.example.wp04.abstd.android.library")
        pluginManager.apply("com.example.wp04.abstd.android.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("api", project(":core:common"))
            add("implementation", project(":core:datastore"))
            add("implementation", project(":core:network"))

            add("implementation", libs.library("kotlinx-serialization-json"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
