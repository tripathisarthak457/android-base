import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HostTestBuilder
import io.example.fitcnet.buildlogic.android.configureAndroidCommon
import io.example.fitcnet.buildlogic.android.configureTestDependencies
import io.example.fitcnet.buildlogic.config.AppConfig
import io.example.fitcnet.buildlogic.guard.verifyCodeRules
import io.example.fitcnet.buildlogic.guard.verifyManifestRules
import io.example.fitcnet.buildlogic.library
import io.example.fitcnet.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Everything an application module needs that is not environment-specific. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        val hasUnitTests = file("src/test").exists()
        val hasDeviceTests = file("src/androidTest").exists()

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)

            defaultConfig.apply {
                targetSdk = AppConfig.TARGET_SDK
                versionCode = AppConfig.VERSION_CODE
                versionName = AppConfig.VERSION_NAME
            }

            buildFeatures.buildConfig = true

            // Libraries bring translations for languages the app does not speak (AndroidX alone
            // brings about eighty); a screen half in one language and half in another is worse
            // than English throughout, and they weigh down resources.arsc.
            androidResources.localeFilters += AppConfig.LOCALES

            // Licence texts and the Kotlin builtins descriptors are read by nothing at runtime.
            packaging.resources.excludes += listOf("META-INF/**/LICENSE*", "kotlin/**.kotlin_builtins")

            buildTypes.getByName("release").apply {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }

            // The app's lint sees every module it depends on, so a check that needs the whole
            // picture (an unused resource, a string missing from one language) runs once, here.
            lint {
                abortOnError = true
                checkDependencies = true
            }
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            beforeVariants { variant ->
                if (!hasUnitTests) variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
                if (!hasDeviceTests) variant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]?.enable = false
            }
            // Layout Inspector reads the AndroidX version files to recognise Compose, so only
            // release builds drop them.
            onVariants(selector().withBuildType("release")) { variant ->
                variant.packaging.resources.excludes.add("/META-INF/*.version")
            }
            verifyManifestRules(this)
        }

        dependencies {
            add("implementation", libs.library("androidx-core-ktx"))
            add("implementation", libs.library("kotlinx-coroutines-android"))
        }

        configureTestDependencies()
        verifyCodeRules()
    }
}
