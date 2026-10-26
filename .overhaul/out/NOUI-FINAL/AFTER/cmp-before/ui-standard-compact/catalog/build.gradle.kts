/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.example.cmp.android.application")
    id("com.example.cmp.android.compose")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.cmp.android.screenshots")
}

android {
    namespace = "com.example.cmp.catalog"

    defaultConfig {
        applicationId = "com.example.cmp.catalog"
    }

    buildTypes {
        release {
            // The catalog never ships, and a release build is the only one whose motion and frame
            // times mean anything, so it is signed with the debug key and installs directly.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    // The Navigation section runs the app's own shell, transitions and back handling.
    implementation(project(":core:navigation"))
    implementation(project(":core:navigation:shell"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)
    // lifecycleScope, which holds the first frame until the fonts are in.
    implementation(libs.androidx.lifecycle.runtime.compose)
}
