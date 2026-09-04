plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
    id("com.base.app.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.base.app.core.navigation"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    // Navigation 3 stops here. Feature modules depend on this module's own AppNavKey /
    // NavGraphEntry types and never see androidx.navigation3 — which is what makes replacing it
    // a change to two files in this module rather than to every feature.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
