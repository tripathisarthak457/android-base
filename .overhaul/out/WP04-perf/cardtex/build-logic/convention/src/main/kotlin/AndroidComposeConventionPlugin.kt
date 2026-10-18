import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.example.wp04.everything.buildlogic.configureCompose
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Compose for a module that already has an Android plugin applied. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension: CommonExtension = extensions.findByType(ApplicationExtension::class.java)
            ?: extensions.findByType(LibraryExtension::class.java)
            ?: throw GradleException(
                "$path applies com.example.wp04.everything.android.compose without an Android plugin. Apply " +
                    "com.example.wp04.everything.android.library or com.example.wp04.everything.android.application first.",
            )

        configureCompose(extension)
    }
}
