plugins {
    id("com.example.wp04.base.android.application")
    id("com.example.wp04.base.android.application.flavors")
    id("com.example.wp04.base.android.compose")
    id("com.example.wp04.base.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.wp04.base.android.firebase")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.wp04.base"

    defaultConfig {
        applicationId = "com.example.wp04.base"
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
    implementation(project(":core:coroutines"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:notification"))
    implementation(project(":core:analytics"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:sample"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:feed"))
    implementation(project(":feature:search"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:orders"))
    implementation(project(":feature:wallet"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))

    implementation(project(":core:flags"))

    implementation(project(":core:devtools"))

    implementation(libs.androidx.glance.appwidget)

    debugImplementation(libs.leakcanary)

    testImplementation(project(":core:testing"))
}

