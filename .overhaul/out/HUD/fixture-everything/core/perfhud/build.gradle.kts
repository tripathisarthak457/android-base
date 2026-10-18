/*
 * The live performance overlay: the pill under the DEV badge, the Performance tab, the log and the
 * report. :app depends on it from dev and debug builds only, so a shippable release does not carry it.
 */
plugins {
    id("com.example.hud.fixture.android.library")
    id("com.example.hud.fixture.android.compose")
    id("com.example.hud.fixture.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.hud.fixture.core.perfhud"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:devtools"))
    implementation(project(":core:analytics"))

    implementation(libs.androidx.metrics.performance)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
}
