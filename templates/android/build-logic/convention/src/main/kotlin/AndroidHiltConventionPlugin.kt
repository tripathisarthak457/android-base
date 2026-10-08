import com.base.app.buildlogic.libs
import com.base.app.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")

        dependencies {
            add("implementation", libs.library("hilt-android"))
            add("ksp", libs.library("hilt-compiler"))

            add("testImplementation", libs.library("hilt-testing"))
            add("kspTest", libs.library("hilt-compiler"))
        }
    }
}
