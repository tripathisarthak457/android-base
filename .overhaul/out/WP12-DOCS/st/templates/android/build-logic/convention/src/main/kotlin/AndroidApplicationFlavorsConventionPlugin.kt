import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.base.app.buildlogic.android.registerDistributionTasks
import com.base.app.buildlogic.config.configureAbiSplits
import com.base.app.buildlogic.config.configureFlavors
import com.base.app.buildlogic.config.configureSigning
import com.base.app.buildlogic.config.configureVariants
// <opt:devtools>
import com.base.app.buildlogic.guard.verifyReleasesCarryNoDevTooling
// </opt:devtools>
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * The four environments — dev, staging, prod, playstore — and everything that follows from them:
 * per-flavour backend URLs, application id suffixes, signing keys, version naming, ABI splits and
 * the named-artifact distribution tasks.
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
        // <opt:devtools>
        verifyReleasesCarryNoDevTooling(components)
        // </opt:devtools>
    }
}
