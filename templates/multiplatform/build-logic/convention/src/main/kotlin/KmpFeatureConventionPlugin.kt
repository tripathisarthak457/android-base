import com.base.app.buildlogic.library
import com.base.app.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: screens, their ViewModels and their navigation entries. Everything a screen
 * needs from core comes with the plugin, so a feature's own build file lists only its data
 * modules.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.base.app.kmp.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:common"))
                implementation(project(":core:mvi"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:ui"))
                implementation(project(":core:navigation"))
                implementation(project.dependencies.platform(libs.library("koin-bom")))
                implementation(libs.library("koin-core"))
                implementation(libs.library("koin-compose-viewmodel"))
                implementation(libs.library("lifecycle-viewmodel-compose"))
                implementation(libs.library("lifecycle-runtime-compose"))
                implementation(libs.library("kotlinx-serialization-json"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":core:testing"))
                implementation(libs.library("turbine"))
            }
        }
    }
}
