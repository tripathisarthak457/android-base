import com.android.build.api.dsl.LibraryExtension
import com.base.app.buildlogic.configureAndroidCommon
import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import com.base.app.buildlogic.registerComposeGuards
import com.base.app.buildlogic.verifyModuleDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Every `:core:*`, `:data:*` and `:feature:*` module. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)

            // Library instrumentation tests only run on debug; building release would cost an extra
            // R8 pass.
            buildTypes.getByName("release").isMinifyEnabled = false
        }

        dependencies {
            add("implementation", libs.library("kotlinx-coroutines-core"))

            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("turbine"))
            add("testImplementation", libs.library("mockk"))
            add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
            add("androidTestImplementation", libs.library("androidx-test-runner"))
        }

        registerComposeGuards()
        verifyModuleDependencies()
    }
}
