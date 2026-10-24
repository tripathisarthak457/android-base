plugins {
    id("com.example.everything.android.data")
}

android {
    namespace = "com.example.everything.data.auth"
}

dependencies {
    implementation(project(":core:network"))
    // Signing in writes the token store directly; a repository in front would only forward.
    implementation(project(":core:datastore"))
}
