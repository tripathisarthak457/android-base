plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.auth"
}

dependencies {
    // Signing in writes the token store directly; a repository in front would only forward.
    implementation(project(":core:datastore"))
}
