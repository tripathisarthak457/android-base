plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    testImplementation(testFixtures(project(":data:auth")))
    // A real Bundle, for the process-death test of the sign-in form.
    testImplementation(libs.robolectric)
}
