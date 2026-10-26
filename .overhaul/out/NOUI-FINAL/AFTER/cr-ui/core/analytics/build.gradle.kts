plugins {
    id("com.example.finalafter.crui.android.library")
    id("com.example.finalafter.crui.android.hilt")
}

android {
    namespace = "com.example.finalafter.crui.core.analytics"
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
