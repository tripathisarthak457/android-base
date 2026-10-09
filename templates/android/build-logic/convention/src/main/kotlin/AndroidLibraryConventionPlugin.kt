import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.base.app.buildlogic.configureAndroidCommon
import com.base.app.buildlogic.configureTestDependencies
import com.base.app.buildlogic.verifyApiPurity
import com.base.app.buildlogic.verifyCodeRules
import com.base.app.buildlogic.verifyModuleDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Every `:core:*`, `:data:*` and `:feature:*` module.
 *
 * Resource processing, unit tests and device tests are switched on only for modules that have the
 * matching source directory: most modules have none of them, and each costs tasks on every build.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        val hasResources = file("src/main/res").exists()
        val hasUnitTests = file("src/test").exists()
        val hasDeviceTests = file("src/androidTest").exists()

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
            androidResources.enable = hasResources
        }

        extensions.configure<LibraryAndroidComponentsExtension> {
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
