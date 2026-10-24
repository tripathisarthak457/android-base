/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.example.deskstandard.android.application")
    id("com.example.deskstandard.android.compose")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.deskstandard.android.screenshots")
}

android {
    namespace = "com.example.deskstandard.catalog"

    defaultConfig {
        applicationId = "com.example.deskstandard.catalog"
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)
    // lifecycleScope, which holds the first frame until the fonts are in.
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Installs the baseline profiles the catalog and its libraries ship, as an app does, so its frame
    // times are measured on code compiled the way an installed app's is.
    implementation(libs.androidx.profileinstaller)
}
