import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.VariantBuilder
import com.example.standardmodular.buildlogic.library
import com.example.standardmodular.buildlogic.libs
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.withType

/**
 * Screenshot tests generated from the module's previews and rendered by Roborazzi under
 * Robolectric: each `@AppScreenPreviews` screen at every window size the annotation lists.
 *
 * Nothing is compared until the goldens exist. `./gradlew recordRoborazziDebug` writes them to
 * `src/test/screenshots/`, and `verifyRoborazziDebug` then fails on any difference. Without either,
 * the unit tests still render every preview, so one that crashes at some size fails the build.
 */
class AndroidScreenshotsConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalRoborazziApi::class)
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")

        extensions.configure<CommonExtension> {
            testOptions.unitTests.isIncludeAndroidResources = true
        }

        // The tests are generated, so a module needs no src/test of its own to run them; debug
        // only, like every other unit test here.
        val enableUnitTests = { variant: VariantBuilder ->
            if (variant.buildType == "debug" && variant is HasHostTestsBuilder) {
                variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
            }
        }
        plugins.withId("com.android.library") {
            extensions.configure<LibraryAndroidComponentsExtension> { beforeVariants { enableUnitTests(it) } }
        }
        plugins.withId("com.android.application") {
            extensions.configure<ApplicationAndroidComponentsExtension> { beforeVariants { enableUnitTests(it) } }
        }

        extensions.configure<RoborazziExtension> {
            outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
            generateComposePreviewRobolectricTests {
                enable.set(true)
                // Only this module's previews: scanning the app's root package would render every
                // dependency's previews again in each module that uses it.
                packages.set(provider { listOf(checkNotNull(extensions.getByType(CommonExtension::class.java).namespace)) })
                includePrivatePreviews.set(true)
                robolectricConfig.set(mapOf("sdk" to "[$ROBOLECTRIC_SDK]"))
            }
        }

        tasks.withType<Test>().configureEach {
            systemProperty("robolectric.graphicsMode", "NATIVE")
            // Captures through the hardware renderer, so shadows and blur match a device.
            systemProperty("robolectric.pixelCopyRenderMode", "hardware")
        }

        // Roborazzi checks that JUnit and Robolectric are declared here, not only inherited.
        dependencies {
            add("testImplementation", project(":core:testing:compose"))
            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("roborazzi"))
            add("testImplementation", libs.library("roborazzi-compose"))
            add("testImplementation", libs.library("roborazzi-preview-scanner"))
            add("testImplementation", libs.library("composable-preview-scanner"))
        }
    }

    private companion object {
        /** `TEST_SDK` in `:core:testing:compose`, which hand-written Robolectric tests use. */
        const val ROBOLECTRIC_SDK = 35
    }
}
