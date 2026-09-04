import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.base.app.buildlogic.configureAbiSplits
import com.base.app.buildlogic.configureFlavors
import com.base.app.buildlogic.configureSigning
import com.base.app.buildlogic.configureVariants
import com.base.app.buildlogic.registerDistributionTasks
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * The four environments — dev, staging, prod, playstore — and everything that follows from them:
 * per-flavour backend URLs, application id suffixes, signing keys, version naming, ABI splits and
 * the named-artifact distribution tasks.
 *
 * Applied by `:app` alone. The order below matters: signing looks the flavours up by name, so
 * they have to exist first.
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
    }
}
