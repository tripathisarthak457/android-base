import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * `:shared`, the module the iOS app links: every feature behind one static framework named
 * `Shared`. Static, so the app launches without a dynamic loader step for it and Xcode embeds
 * nothing extra.
 */
class KmpUmbrellaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.base.app.kmp.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType(KotlinNativeTarget::class.java).configureEach {
                binaries.framework {
                    baseName = FRAMEWORK_NAME
                    isStatic = true
                }
            }
        }
    }

    private companion object {
        const val FRAMEWORK_NAME = "Shared"
    }
}
