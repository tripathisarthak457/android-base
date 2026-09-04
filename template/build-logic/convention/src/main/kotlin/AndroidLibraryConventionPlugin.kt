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

/**
 * Every `:core:*`, `:data:*` and `:feature:*` module.
 *
 * Library modules carry no product flavours on purpose — see [com.base.app.buildlogic.configureFlavors]
 * for why. They also do not enable `buildConfig`: a library that reads `BuildConfig.DEBUG` is a
 * library that behaves differently depending on who compiled it, which is exactly the bug you
 * cannot reproduce. What varies by environment is injected instead.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)

            // The instrumentation test APK of a library is built for the debug variant only, and
            // building the release one costs a full extra R8 pass per module for artifacts nobody
            // runs.
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
