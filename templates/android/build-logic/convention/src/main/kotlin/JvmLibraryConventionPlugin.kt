import com.base.app.buildlogic.config.AppConfig
import com.base.app.buildlogic.guard.verifyApiPurity
import com.base.app.buildlogic.guard.verifyCodeRules
import com.base.app.buildlogic.guard.verifyModuleDependencies
import com.base.app.buildlogic.library
import com.base.app.buildlogic.libs
import com.base.app.buildlogic.limitTestTime
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** A plain Kotlin module, with no Android in it at all: `:core:common`, every `:api` and `:domain:` module. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        // A modular data domain's fakes sit beside the interfaces they implement, in its `:api`.
        if (file("src/testFixtures").exists()) {
            pluginManager.apply("java-test-fixtures")
        }

        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(AppConfig.JVM_TARGET)
            }
        }

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = AppConfig.JAVA_VERSION
            targetCompatibility = AppConfig.JAVA_VERSION
        }

        if (file("src/test").exists()) {
            dependencies {
                add("testImplementation", libs.library("junit"))
            }
        }
        limitTestTime()

        verifyCodeRules()
        verifyModuleDependencies()
        verifyApiPurity()
    }
}
