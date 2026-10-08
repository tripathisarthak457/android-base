plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))

    // <opt:googlesignin>
    // Credential Manager is the one API for passwords, passkeys and Google accounts; the
    // play-services artifact is what makes it work below Android 14.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    // </opt:googlesignin>
}
