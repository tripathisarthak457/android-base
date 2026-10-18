/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.example.hud.lean.android.application")
    id("com.example.hud.lean.android.compose")
}
android {
    namespace = "com.example.hud.lean.catalog"

    defaultConfig {
        applicationId = "com.example.hud.lean.catalog"
        versionCode = 1
        versionName = "1.0.0"
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
    implementation(libs.androidx.lifecycle.runtime.compose)

}

