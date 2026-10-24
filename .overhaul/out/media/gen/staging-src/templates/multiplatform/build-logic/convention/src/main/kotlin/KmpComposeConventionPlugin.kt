import com.base.app.buildlogic.kmp.configureComposeMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Compose on top of [KmpLibraryConventionPlugin]. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.base.app.kmp.library")
        configureComposeMultiplatform()
    }
}
