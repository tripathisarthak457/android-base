plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.hilt")
    id("com.example.cmp.android.compose")
    alias(libs.plugins.kotlin.serialization)
    // MultiInstanceTest's activity is an @AndroidEntryPoint, which the Hilt plugin rewrites.
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.cmp.core.navigation"
    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    compilerOptions {
        // The host API is this module's own; only a host in another module has to opt in to it.
        optIn.add("com.example.cmp.core.navigation.NavigationHostApi")
    }
}

dependencies {
    // AppNavKey is in every signature of this module's API.
    api(project(":core:common"))

    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // The host draws the stack with Navigation 3's NavDisplay. Features never see androidx.navigation3:
    // they depend on AppNavKey and NavGraphEntry.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.animation)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
