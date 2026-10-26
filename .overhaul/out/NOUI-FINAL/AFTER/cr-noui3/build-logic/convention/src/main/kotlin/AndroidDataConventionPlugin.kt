import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Every `:data:*` module: one business domain end to end, its DTOs, mappers and repository,
 * depending only on `:core:*`. A module adds `:core:network`, `:core:datastore` or `:core:database`
 * itself when it uses them, so a change in one of them recompiles only the domains that need it.
 */
class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.example.finalafter.crnoui3.android.library")
        pluginManager.apply("com.example.finalafter.crnoui3.android.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            // The api package exposes AppResult and AppError, so callers need them too.
            add("api", project(":core:common"))
        }
    }
}
