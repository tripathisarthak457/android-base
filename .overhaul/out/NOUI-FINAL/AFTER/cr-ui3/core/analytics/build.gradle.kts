plugins {
    id("com.example.finalafter.crui3.android.library")
    id("com.example.finalafter.crui3.android.hilt")
}

android {
    namespace = "com.example.finalafter.crui3.core.analytics"
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
