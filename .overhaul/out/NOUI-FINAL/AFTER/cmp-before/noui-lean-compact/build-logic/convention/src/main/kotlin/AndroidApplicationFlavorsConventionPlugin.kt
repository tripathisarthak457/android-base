import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.example.cmp.buildlogic.android.registerDistributionTasks
import com.example.cmp.buildlogic.config.configureAbiSplits
import com.example.cmp.buildlogic.config.configureFlavors
import com.example.cmp.buildlogic.config.configureSigning
import com.example.cmp.buildlogic.config.configureVariants
import com.example.cmp.buildlogic.guard.guardReleaseDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * The four environments — dev, staging, prod, playstore — and everything that follows from them:
 * per-flavour backend URLs, application id suffixes, signing keys, version naming, ABI splits,
 * the named-artifact distribution tasks, and the guarded list of what the prod release ships.
 */
class AndroidApplicationFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.configure<ApplicationExtension> {
            configureFlavors(this)
            configureSigning(this)
            configureAbiSplits(this)
        }

        val components = extensions.getByType<ApplicationAndroidComponentsExtension>()
        configureVariants(components)
        registerDistributionTasks(components)
        guardReleaseDependencies()
    }
}
