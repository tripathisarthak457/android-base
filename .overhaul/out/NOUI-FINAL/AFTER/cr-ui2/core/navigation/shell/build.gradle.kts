/*
 * The navigation shell: the tab bar, rail and sidebar, the stack host and its slides, the back card,
 * sheets and covers, all drawn with the design system over :core:navigation's runtime. Only the
 * composition roots depend on it; a feature sees the runtime alone.
 */
plugins {
    id("com.example.finalafter.crui2.android.library")
    id("com.example.finalafter.crui2.android.compose")
    // The tests' keys are @Serializable, as every destination's is.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.finalafter.crui2.core.navigation.shell"
    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    compilerOptions {
        // The shell is a navigation host: it applies commands to the runtime's stacks and draws their entries.
        optIn.add("com.example.finalafter.crui2.core.navigation.NavigationHostApi")
    }
}

dependencies {
    // AppNavigator, NavRegistry and the back stacks are in the shell's signatures.
    api(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing"))
    // MviScreen and MviViewModel, only for the test where a screen's first load meets tab warm-up.
    testImplementation(project(":core:ui"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
