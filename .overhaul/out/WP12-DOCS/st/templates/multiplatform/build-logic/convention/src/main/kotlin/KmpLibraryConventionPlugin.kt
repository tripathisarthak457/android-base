import com.base.app.buildlogic.guard.verifyApiPurity
import com.base.app.buildlogic.guard.verifyModuleDependencies
import com.base.app.buildlogic.kmp.addCoroutinesTest
import com.base.app.buildlogic.kmp.configureKmpLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project

/** A multiplatform library for Android and iOS. Every module but `:core:common` starts here. */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        configureKmpLibrary()
        addCoroutinesTest()
        verifyModuleDependencies()
        verifyApiPurity()
    }
}
