/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.base.app.android.application")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.catalog"

    defaultConfig {
        applicationId = "com.base.app.catalog"
    }

    buildTypes {
        release {
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
    // lifecycleScope, which holds the first frame until the fonts are in.
    implementation(libs.androidx.lifecycle.runtime.compose)
}
