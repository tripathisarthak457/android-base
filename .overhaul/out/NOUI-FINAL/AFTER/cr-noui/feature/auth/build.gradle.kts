plugins {
    id("com.example.finalafter.crnoui.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    // Credential Manager is the one API for passwords, passkeys and Google accounts; the
    // play-services artifact is what makes it work below Android 14.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(testFixtures(project(":data:auth")))
    // A real Bundle, for the process-death test of the sign-in form.
    testImplementation(libs.robolectric)
}
