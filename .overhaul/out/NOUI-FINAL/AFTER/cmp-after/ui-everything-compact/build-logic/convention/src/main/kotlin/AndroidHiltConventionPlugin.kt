import com.example.cmp.buildlogic.library
import com.example.cmp.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt for a module that declares `@Module`s, `@Inject` constructors or `@HiltViewModel`s.
 *
 * The Hilt Gradle plugin is applied only to application modules, where the entry points
 * (`@HiltAndroidApp`, `@AndroidEntryPoint`) live: a library needs only the runtime and the
 * annotation processor, and the plugin's bytecode transform would run on every variant for nothing.
 * A library that declares an `@AndroidEntryPoint` applies `alias(libs.plugins.hilt)` itself.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.withPlugin("com.android.application") {
            pluginManager.apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", libs.library("hilt-android"))
            add("ksp", libs.library("hilt-compiler"))
        }
    }
}
