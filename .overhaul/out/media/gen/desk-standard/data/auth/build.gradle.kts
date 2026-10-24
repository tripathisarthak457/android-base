plugins {
    id("com.example.deskstandard.android.data")
}

android {
    namespace = "com.example.deskstandard.data.auth"
}

dependencies {
    implementation(project(":core:network"))
    // Signing in writes the token store directly; a repository in front would only forward.
    implementation(project(":core:datastore"))
}
