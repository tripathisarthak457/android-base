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
    // <opt:screenshottests>
    alias(libs.plugins.roborazzi)
    // </opt:screenshottests>
}

android {
    namespace = "com.base.app.catalog"

    defaultConfig {
        applicationId = "com.base.app.catalog"
        versionCode = 1
        versionName = "1.0.0"
    }

    // <opt:screenshottests>
    // Robolectric needs the merged resources and manifest to render anything at all.
    testOptions.unitTests.isIncludeAndroidResources = true
    // </opt:screenshottests>

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

    // <opt:screenshottests>
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    // </opt:screenshottests>
}

// <opt:screenshottests>
/*
 * `./gradlew build` compares the rendered components against the images in src/test/screenshots.
 *
 * Wired into `check` rather than left as a task somebody remembers to run, because a screenshot
 * suite nobody runs is worse than none: it goes stale, everyone learns to re-record it without
 * looking, and the one time it was right it gets overwritten.
 *
 * Guarded on the images existing because a generated project starts without them — they are
 * recorded against a palette and a typeface, and this project has its own. Record the baseline
 * once and `check` picks the suite up from then on:
 *
 *     ./gradlew :catalog:recordRoborazziDebug
 */
if (file("src/test/screenshots").list()?.isNotEmpty() == true) {
    tasks.named("check") { dependsOn("verifyRoborazziDebug") }
}
// </opt:screenshottests>
