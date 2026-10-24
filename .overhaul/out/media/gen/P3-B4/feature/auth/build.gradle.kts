plugins {
    id("com.example.everything.android.feature")
}

android {
    namespace = "com.example.everything.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    // Credential Manager is the one API for passwords, passkeys and Google accounts; the
    // play-services artifact is what makes it work below Android 14.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // A real Bundle, for the process-death test of the sign-in form.
    testImplementation(libs.robolectric)
}
