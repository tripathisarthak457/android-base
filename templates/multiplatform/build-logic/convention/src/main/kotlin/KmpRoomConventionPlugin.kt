import androidx.room3.gradle.RoomExtension
import com.base.app.buildlogic.library
import com.base.app.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Room 3 with the bundled SQLite driver, so the database behaves the same on both platforms. The
 * processor runs once per target: KSP's plain `ksp(...)` configuration is deprecated in
 * multiplatform projects and would only process common code.
 */
class KmpRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room3")

        extensions.configure<RoomExtension> {
            // Checked in, so every migration is tested against the schema it starts from.
            schemaDirectory("$projectDir/schemas")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                api(libs.library("androidx-room3-runtime"))
                implementation(libs.library("androidx-sqlite-bundled"))
            }
        }

        dependencies {
            val compiler = libs.library("androidx-room3-compiler")
            add("kspAndroid", compiler)
            add("kspIosArm64", compiler)
            add("kspIosSimulatorArm64", compiler)
        }
    }
}
