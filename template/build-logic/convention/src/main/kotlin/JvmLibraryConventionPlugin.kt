import com.base.app.buildlogic.AppConfig
import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import com.base.app.buildlogic.verifyModuleDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * A plain Kotlin module, with no Android in it at all.
 *
 * Used by `:core:model`, whose contents are data classes and nothing else. Skipping the Android
 * plugin there is not a purity argument: it removes resource merging, manifest processing and
 * variant computation from the module that sits at the very bottom of the graph and therefore
 * gets rebuilt more often than any other. Its tests are plain JUnit that run in milliseconds.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(AppConfig.JVM_TARGET)
                freeCompilerArgs.add("-Xannotation-default-target=param-property")
            }
        }

        extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
            sourceCompatibility = AppConfig.JAVA_VERSION
            targetCompatibility = AppConfig.JAVA_VERSION
        }

        tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
            failOnNoDiscoveredTests.set(false)
        }

        dependencies {
            add("testImplementation", libs.library("junit"))
        }

        verifyModuleDependencies()
    }
}
