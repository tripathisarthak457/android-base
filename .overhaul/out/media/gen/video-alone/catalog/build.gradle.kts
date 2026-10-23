/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.example.videoalone.android.application")
    id("com.example.videoalone.android.compose")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.videoalone.android.screenshots")
}

android {
    namespace = "com.example.videoalone.catalog"

    defaultConfig {
        applicationId = "com.example.videoalone.catalog"
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
    // The photo scenes run the app's own image component and loader, with its limits.
    implementation(project(":core:image"))
    // The short-video scene runs the app's own feed state and player surface.
    implementation(project(":core:video"))
}
