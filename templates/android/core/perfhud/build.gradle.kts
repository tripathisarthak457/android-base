/*
 * The live performance overlay: the pill under the DEV badge, the Performance tab, the log and the
 * report. :app depends on it from dev and debug builds only, so a shippable release does not carry it.
 */
plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.base.app.core.perfhud"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:devtools"))
    // <opt:analytics>
    implementation(project(":core:analytics"))
    // </opt:analytics>

    implementation(libs.androidx.metrics.performance)
    implementation(libs.kotlinx.serialization.json)
    // <opt:baselineprofile>
    implementation(libs.androidx.profileinstaller)
    // </opt:baselineprofile>

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
}
