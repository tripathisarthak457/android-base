import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.example.standard.buildlogic.android.configureCompose
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Compose for a module that already has an Android plugin applied. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension: CommonExtension = extensions.findByType(ApplicationExtension::class.java)
            ?: extensions.findByType(LibraryExtension::class.java)
            ?: throw GradleException(
                "$path applies com.example.standard.android.compose without an Android plugin. Apply " +
                    "com.example.standard.android.library or com.example.standard.android.application first.",
            )

        configureCompose(extension)
    }
}
