plugins {
    id("com.example.wp04.abstd.android.application")
    id("com.example.wp04.abstd.android.application.flavors")
    id("com.example.wp04.abstd.android.compose")
    id("com.example.wp04.abstd.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.wp04.abstd"

    defaultConfig {
        applicationId = "com.example.wp04.abstd"
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

    implementation(project(":feature:auth"))
    implementation(project(":feature:sample"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Installs the checked-in baseline profiles on first launch for sideloaded and pre-Play installs;
    // every preset ships those profiles, with or without the benchmark module.
    implementation(libs.androidx.profileinstaller)

    implementation(project(":core:flags"))

    implementation(project(":core:devtools"))

    debugImplementation(libs.leakcanary)

    testImplementation(project(":core:testing"))
}

