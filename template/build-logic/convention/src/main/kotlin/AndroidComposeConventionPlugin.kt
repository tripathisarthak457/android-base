import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.base.app.buildlogic.configureCompose
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Compose for a module that already has an Android plugin applied.
 *
 * Works for both application and library modules; which one it is only affects where the
 * extension is read from.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension: CommonExtension = extensions.findByType(ApplicationExtension::class.java)
            ?: extensions.findByType(LibraryExtension::class.java)
            ?: throw GradleException(
                "$path applies com.base.app.android.compose without an Android plugin. Apply " +
                    "com.base.app.android.library or com.base.app.android.application first.",
            )

        configureCompose(extension)
    }
}
