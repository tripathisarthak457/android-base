/*
 * The design-system catalog: every component, every state, on a device.
 */

plugins {
    id("com.example.wp04.dlplay.android.application")
    id("com.example.wp04.dlplay.android.compose")
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "com.example.wp04.dlplay.catalog"

    defaultConfig {
        applicationId = "com.example.wp04.dlplay.catalog"
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
    // The Navigation section runs the app's own shell, transitions and back handling.
    implementation(project(":core:navigation"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

}

