plugins {
    id("com.example.hud.standard.android.application")
    id("com.example.hud.standard.android.application.flavors")
    id("com.example.hud.standard.android.compose")
    id("com.example.hud.standard.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.hud.standard"

    defaultConfig {
        applicationId = "com.example.hud.standard"
    }

    // Writes the android:localeConfig the phone's per-app language setting reads, from the
    // values-xx directories that actually exist. The default language is in resources.properties.
    androidResources {
        generateLocaleConfig = true
    }

    buildTypes {
        release {
            // `getDefaultProguardFile` is only reachable from a build script, which is why this one
            // block stays here rather than in the convention plugin.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))

    implementation(project(":feature:sample"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(project(":core:flags"))

    implementation(project(":core:devtools"))
    // Dev and debug builds only: a shippable release (prodRelease, playstoreRelease) has no trace of
    // the overlay, not one hidden behind a flag.
    debugImplementation(project(":core:perfhud"))
    "devImplementation"(project(":core:perfhud"))

    implementation(project(":core:database"))

    debugImplementation(libs.leakcanary)

    testImplementation(project(":core:testing"))
}

