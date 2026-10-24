import androidx.room3.gradle.RoomExtension
import io.example.audit.buildlogic.library
import io.example.audit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room 3, with each version's schema exported to `schemas/` beside the module. Commit that
 * directory: a migration test reads the old version's schema from it, and without it there is
 * nothing to migrate from.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room3")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        dependencies {
            // `api`: the database class extends RoomDatabase, so a data module that asks it for a
            // DAO compiles against Room too.
            add("api", libs.library("androidx-room3-runtime"))
            add("implementation", libs.library("androidx-sqlite-framework"))
            add("ksp", libs.library("androidx-room3-compiler"))
        }
    }
}
