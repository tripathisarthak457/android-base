plugins {
    id("com.example.wp04.everything.android.library")
    id("com.example.wp04.everything.android.hilt")
}

android {
    namespace = "com.example.wp04.everything.core.analytics"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    testImplementation(project(":core:testing"))
}
