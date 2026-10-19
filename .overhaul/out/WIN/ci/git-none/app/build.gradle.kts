plugins {
    id("com.example.cicheck.android.application")
    id("com.example.cicheck.android.application.flavors")
    id("com.example.cicheck.android.compose")
    id("com.example.cicheck.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.cicheck"

    defaultConfig {
        applicationId = "com.example.cicheck"
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

    implementation(project(":feature:orders"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
}

