plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.hilt")
}

android {
    namespace = "com.example.cmp.core.analytics"
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
