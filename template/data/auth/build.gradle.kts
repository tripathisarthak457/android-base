plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.auth"
}

dependencies {
    // The one data module that writes the token store directly: signing in *is* the act of
    // putting tokens there, so putting a repository in front of it would be a layer that only
    // forwards, and a second place tokens could be written from.
    implementation(project(":core:datastore"))
}
