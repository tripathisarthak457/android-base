plugins {
    id("com.base.app.android.application")
    id("com.base.app.android.application.flavors")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    // <opt:firebase>
    id("com.base.app.android.firebase")
    // </opt:firebase>
    // <opt:baselineprofile>
    alias(libs.plugins.baselineprofile)
    // </opt:baselineprofile>
}

android {
    namespace = "com.base.app"

    defaultConfig {
        applicationId = "com.base.app"
    }

    buildTypes {
        release {
            // `getDefaultProguardFile` is only reachable from a build script, which is why this
            // one block stays here rather than in the convention plugin. The optimised variant
            // of the default file is the one worth having: it enables the class-merging and
            // inlining passes that the plain file leaves off.
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
    // <opt:network>
    implementation(project(":core:network"))
    // </opt:network>
    // <opt:push>
    implementation(project(":core:notification"))
    // </opt:push>
    // <opt:analytics>
    implementation(project(":core:analytics"))
    // </opt:analytics>

    // <opt:auth>
    implementation(project(":feature:auth"))
    // </opt:auth>
    // <opt:sample>
    implementation(project(":feature:sample"))
    // </opt:sample>
    // <opt:settings>
    implementation(project(":feature:settings"))
    // </opt:settings>
    // <opt:onboarding>
    implementation(project(":feature:onboarding"))
    // </opt:onboarding>
    // <generated:app-feature-dependencies>

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // <opt:workmanager>
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // </opt:workmanager>

    // <opt:coil>
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // </opt:coil>

    // <opt:baselineprofile>
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))
    // </opt:baselineprofile>

    // <opt:leakcanary>
    debugImplementation(libs.leakcanary)
    // </opt:leakcanary>

    testImplementation(project(":core:testing"))
}
