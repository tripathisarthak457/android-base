plugins {
    id("com.example.wp04.everything.android.data")
}

android {
    namespace = "com.example.wp04.everything.data.auth"
}

dependencies {
    // Signing in writes the token store directly; a repository in front would only forward.
    implementation(project(":core:datastore"))
}
