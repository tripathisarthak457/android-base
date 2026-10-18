/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.example.wp04.everything.android.application")
    id("com.example.wp04.everything.android.compose")
}

android {
    namespace = "com.example.wp04.everything.catalog"

    defaultConfig {
        applicationId = "com.example.wp04.everything.catalog"
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)
    // lifecycleScope, which holds the first frame until the fonts are in.
    implementation(libs.androidx.lifecycle.runtime.compose)
}
