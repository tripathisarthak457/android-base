plugins {
    id("com.example.hud.everything.android.application")
    id("com.example.hud.everything.android.application.flavors")
    id("com.example.hud.everything.android.compose")
    id("com.example.hud.everything.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.hud.everything.android.firebase")
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.licensee)
    id("com.example.hud.everything.android.licenses")
}

android {
    namespace = "com.example.hud.everything"

    defaultConfig {
        applicationId = "com.example.hud.everything"
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
    implementation(project(":core:notification"))
    implementation(project(":core:applock"))
    implementation(project(":core:playstore"))
    implementation(project(":core:analytics"))

    implementation(project(":feature:sample"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:feed"))
    implementation(project(":feature:search"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))

    implementation(project(":core:flags"))

    implementation(project(":core:devtools"))

    implementation(project(":core:database"))

    debugImplementation(libs.leakcanary)

    testImplementation(project(":core:testing"))
}

/*
 * What this app is allowed to ship. Licensee fails the build on anything not listed, which turns
 * "we shipped a copyleft dependency" from a discovery into a build error at the moment the
 * dependency is added.
 */
licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allow("EPL-1.0")
    allow("CC0-1.0")

    // These three state their licence as a URL rather than an SPDX id, so each is allowed by hand.
    allowUrl("https://developer.android.com/studio/terms.html") {
        because("The Android Software Development Kit License, on Google's own artifacts.")
    }
    allowUrl("https://developer.android.com/guide/playcore/license") {
        because("The Play Core Software Development Kit Terms of Service.")
    }
    allowUrl("https://opensource.org/license/mit") {
        because("MIT, stated as a URL rather than as an identifier — slf4j-api does this.")
    }
}
