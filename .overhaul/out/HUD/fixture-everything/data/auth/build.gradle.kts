plugins {
    id("com.example.hud.fixture.android.data")
}

android {
    namespace = "com.example.hud.fixture.data.auth"
}

dependencies {
    implementation(project(":core:network"))
    // Signing in writes the token store directly; a repository in front would only forward.
    implementation(project(":core:datastore"))
}
