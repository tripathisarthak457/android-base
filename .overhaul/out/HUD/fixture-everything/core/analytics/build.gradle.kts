plugins {
    id("com.example.hud.fixture.android.library")
    id("com.example.hud.fixture.android.hilt")
}

android {
    namespace = "com.example.hud.fixture.core.analytics"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    testImplementation(project(":core:testing"))
}
