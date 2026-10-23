plugins {
    id("com.example.standard.android.feature")
}

android {
    namespace = "com.example.standard.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    // A real Bundle, for the process-death test of the sign-in form.
    testImplementation(libs.robolectric)
}
