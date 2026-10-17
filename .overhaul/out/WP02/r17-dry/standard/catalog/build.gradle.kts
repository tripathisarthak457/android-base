/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.example.standard.android.application")
    id("com.example.standard.android.compose")
    id("com.example.standard.android.screenshots")
}
android {
    namespace = "com.example.standard.catalog"

    defaultConfig {
        applicationId = "com.example.standard.catalog"
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

