import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.example.standard.buildlogic.android.configureAndroidCommon
import com.example.standard.buildlogic.android.configureTestDependencies
import com.example.standard.buildlogic.guard.verifyApiPurity
import com.example.standard.buildlogic.guard.verifyCodeRules
import com.example.standard.buildlogic.guard.verifyModuleDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Every `:core:*`, `:data:*` and `:feature:*` module.
 *
 * Resource processing, unit tests, device tests and test fixtures are switched on only for modules
 * that have the matching source directory: most modules have none of them, and each costs tasks on
 * every build.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        val hasResources = file("src/main/res").exists()
        val hasUnitTests = file("src/test").exists()
        val hasDeviceTests = file("src/androidTest").exists()
        // Fakes that other modules' tests share through `testFixtures(project(…))`.
        val hasTestFixtures = file("src/testFixtures").exists()

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
            androidResources.enable = hasResources
            testFixtures.enable = hasTestFixtures
        }

        extensions.configure<LibraryAndroidComponentsExtension> {
            // Resource names share one namespace across the app, so each module prefixes its own
            // with its name (`orders_title`), and lint's ResourceName holds it to that. The
            // namespace is set by the module's build file, after this convention has run.
            if (hasResources) {
                finalizeDsl { library ->
                    library.resourcePrefix = checkNotNull(library.namespace) { "$path sets no namespace" }
                        .substringAfterLast('.') + "_"
                }
            }
            beforeVariants { variant ->
                // Release compiles the same sources as debug, so its unit tests would only run them twice.
                variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = hasUnitTests && variant.buildType == "debug"
                variant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]?.enable = hasDeviceTests
            }
        }

        configureTestDependencies()
        verifyCodeRules()
        verifyModuleDependencies()
        verifyApiPurity()
    }
}
