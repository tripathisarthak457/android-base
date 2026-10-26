/*
 * The live performance overlay: the pill under the DEV badge, the Performance tab, the log and the
 * report. :app depends on it from dev and debug builds only, so a shippable release does not carry it.
 */
plugins {
    id("com.example.finalafter.crnoui.android.library")
    id("com.example.finalafter.crnoui.android.compose")
    id("com.example.finalafter.crnoui.android.hilt")
    // ReportReceiver is an @AndroidEntryPoint, which the Hilt plugin rewrites.
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.finalafter.crnoui.core.perfhud"

    // The pill's redraw test composes under Robolectric, which needs the merged manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:devtools"))
    implementation(project(":core:analytics"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.metrics.performance)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    // DisplayManager is a final framework class, so FrameIntakeTest mocks it rather than faking it.
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
