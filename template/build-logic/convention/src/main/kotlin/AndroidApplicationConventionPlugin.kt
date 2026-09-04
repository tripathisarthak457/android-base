import com.android.build.api.dsl.ApplicationExtension
import com.base.app.buildlogic.AppConfig
import com.base.app.buildlogic.configureAndroidCommon
import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import com.base.app.buildlogic.registerComposeGuards
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Everything an application module needs that is not environment-specific.
 *
 * Product flavours, signing and release packaging live in a separate plugin
 * ([AndroidApplicationFlavorsConventionPlugin]) so that `:catalog` — which is an application but
 * ships to nobody — can take this without inheriting a four-flavour variant matrix it has no use
 * for.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)

            defaultConfig.apply {
                targetSdk = AppConfig.TARGET_SDK
                versionCode = AppConfig.VERSION_CODE
                versionName = AppConfig.VERSION_NAME
            }

            buildFeatures.buildConfig = true

            buildTypes.getByName("release").apply {
                isMinifyEnabled = true
                isShrinkResources = true
            }

            buildTypes.getByName("debug").apply {
                isMinifyEnabled = false
                isShrinkResources = false
            }
        }

        dependencies {
            add("implementation", libs.library("androidx-core-ktx"))
            add("implementation", libs.library("kotlinx-coroutines-android"))

            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
            add("androidTestImplementation", libs.library("androidx-test-runner"))
        }

        registerComposeGuards()
    }
}
