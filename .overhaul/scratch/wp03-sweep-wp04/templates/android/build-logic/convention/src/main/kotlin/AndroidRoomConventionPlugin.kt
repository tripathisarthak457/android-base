import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room, with its schemas exported to a directory that is meant to be committed. */
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
