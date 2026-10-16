import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.base.app.buildlogic.config.AppConfig
import com.base.app.buildlogic.config.AppFlavor
import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * The Android app: an Activity that hosts the shared UI, its flavours and its release build. AGP 9
 * compiles Kotlin itself and will not run alongside the multiplatform plugin in one module, which
 * is why the app is a module of its own and every library is multiplatform.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ApplicationExtension> {
            namespace = AppConfig.BASE_PACKAGE
            compileSdk = AppConfig.COMPILE_SDK

            defaultConfig.apply {
                applicationId = AppConfig.APPLICATION_ID
                minSdk = AppConfig.MIN_SDK
                targetSdk = AppConfig.TARGET_SDK
                versionCode = AppConfig.VERSION_CODE
                versionName = AppConfig.VERSION_NAME
            }

            buildFeatures.compose = true
            buildFeatures.buildConfig = true

            flavorDimensions += AppFlavor.DIMENSION
            AppFlavor.entries.forEach { flavor ->
                productFlavors.create(flavor.flavorName).apply {
                    dimension = AppFlavor.DIMENSION
                    flavor.applicationIdSuffix?.let { applicationIdSuffix = it }
                    buildConfigField("String", "API_BASE_URL", "\"${flavor.apiBaseUrl}\"")
                    buildConfigField("String", "ENVIRONMENT", "\"${flavor.flavorName}\"")
                }
            }

            buildTypes.getByName("release").apply {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }

            compileOptions.apply {
                sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions { jvmTarget.set(AppConfig.JVM_TARGET) }
        }

        extensions.configure<ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFiles.add(
                rootProject.layout.projectDirectory.file("config/compose-stability.conf"),
            )
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val shipped = variant.flavorName == AppFlavor.Prod.flavorName &&
                    variant.buildType == "release"
                val versionName = if (shipped) {
                    AppConfig.VERSION_NAME
                } else {
                    "${AppConfig.VERSION_NAME}-${variant.name}"
                }
                variant.outputs.forEach { it.versionName.set(versionName) }
            }
        }

        dependencies {
            add("implementation", libs.library("androidx-activity-compose"))
            add("implementation", libs.library("kotlinx-coroutines-android"))
            add("debugImplementation", libs.library("compose-ui-tooling"))
        }
    }
}
