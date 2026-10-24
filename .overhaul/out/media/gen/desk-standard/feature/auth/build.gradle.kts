plugins {
    id("com.example.deskstandard.android.feature")
}

android {
    namespace = "com.example.deskstandard.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    // A real Bundle, for the process-death test of the sign-in form.
    testImplementation(libs.robolectric)
}
