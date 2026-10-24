import com.base.app.buildlogic.guard.verifyModuleDependencies
import com.base.app.buildlogic.kmp.addCoroutinesTest
import com.base.app.buildlogic.kmp.configureKmpPure
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Pure Kotlin for every target, with no Android or Compose plugin: `:core:common`. */
class KmpPureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        configureKmpPure()
        addCoroutinesTest()
        verifyModuleDependencies()
    }
}
