plugins {
    id("com.example.standardmodular.android.library")
    id("com.example.standardmodular.android.hilt")
    id("com.example.standardmodular.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.standardmodular.core.navigation"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    // Only this module sees androidx.navigation3; features depend on AppNavKey and NavGraphEntry.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.activity.compose)

    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
