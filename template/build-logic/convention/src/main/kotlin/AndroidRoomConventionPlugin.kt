import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room, with its schemas exported to a directory that is meant to be committed.
 *
 * The exported JSON is what `MigrationTestHelper` reads to open a database at an older version
 * and replay a migration against it. Without it a migration can only be tested by upgrading a
 * device by hand, which is to say it is not tested. The schema directory is wired through a KSP
 * argument rather than the Room Gradle plugin — it is the same result, one fewer plugin on the
 * classpath, and one fewer thing to keep version-aligned.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "${projectDir}/schemas")
            arg("room.generateKotlin", "true")
        }

        dependencies {
            add("implementation", libs.library("androidx-room-runtime"))
            add("implementation", libs.library("androidx-room-ktx"))
            add("ksp", libs.library("androidx-room-compiler"))
            add("testImplementation", libs.library("androidx-room-testing"))
        }
    }
}
