plugins {
    id("com.example.wp04.abstd.android.feature")
}

android {
    namespace = "com.example.wp04.abstd.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    // A real Bundle, for the process-death test of the sign-in form.
    testImplementation(libs.robolectric)
}
