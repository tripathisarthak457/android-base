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
    // <opt:licenses>
    alias(libs.plugins.licensee)
    id("com.base.app.android.licenses")
    // </opt:licenses>
}

android {
    namespace = "com.base.app"

    defaultConfig {
        applicationId = "com.base.app"
    }

    // <opt:language>
    // Writes the android:localeConfig the phone's per-app language setting reads, from the
    // values-xx directories that actually exist. The default language is in resources.properties.
    androidResources {
        generateLocaleConfig = true
    }
    // </opt:language>

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
    // <opt:paging>
    implementation(project(":feature:feed"))
    // </opt:paging>
    // <opt:search>
    implementation(project(":feature:search"))
    // </opt:search>
    // <opt:profile>
    implementation(project(":feature:profile"))
    // </opt:profile>
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

    // <opt:flags>
    implementation(project(":core:flags"))
    // </opt:flags>

    // <opt:devtools>
    implementation(project(":core:devtools"))
    // </opt:devtools>

    // <opt:database>
    implementation(project(":core:database"))
    // </opt:database>

    // <opt:applock>
    implementation(libs.androidx.biometric)
    // </opt:applock>

    // <opt:playstore>
    implementation(libs.play.app.update)
    implementation(libs.play.app.review)
    // </opt:playstore>

    // <opt:widget>
    implementation(libs.androidx.glance.appwidget)
    // </opt:widget>

    // <opt:leakcanary>
    debugImplementation(libs.leakcanary)
    // </opt:leakcanary>

    testImplementation(project(":core:testing"))
}

// <opt:licenses>
/*
 * What this app is allowed to ship.
 *
 * Licensee fails the build on anything not listed, which turns "we shipped a copyleft dependency"
 * from a discovery into a build error at the moment the dependency is added. The list below is
 * the permissive set; adding to it should be a decision somebody makes deliberately, which is
 * exactly why it is here and not hidden in a plugin default.
 */
licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allow("EPL-1.0")
    allow("CC0-1.0")

    // Three real dependencies state their terms as a URL rather than as an SPDX identifier,
    // so each one has to be allowed by hand. That is the plugin working: an unrecognised
    // licence stops the build until somebody has actually looked at it.
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
// </opt:licenses>
