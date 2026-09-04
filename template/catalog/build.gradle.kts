/*
 * The design-system catalog: every component, every state, on a device.
 *
 * Its own application rather than a screen inside the app, for two reasons. It installs beside
 * the real app so a designer or a tester can hold both at once, and it depends on
 * `:core:designsystem` alone — so iterating on a component recompiles two modules instead of the
 * whole graph. No flavours: there is no environment for a catalog to point at.
 */

plugins {
    id("com.base.app.android.application")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.catalog"

    defaultConfig {
        applicationId = "com.base.app.catalog"
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
