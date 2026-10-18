plugins {
    id("com.example.wp04.base.android.feature")
}

android {
    namespace = "com.example.wp04.base.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    // Credential Manager is the one API for passwords, passkeys and Google accounts; the
    // play-services artifact is what makes it work below Android 14.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
