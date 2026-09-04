plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.settings"
}

dependencies {
    // Settings is the one screen that reads the preference store directly rather than through a
    // :data: module — the values are the app's own configuration, not a business domain, and
    // wrapping them in a repository would be a layer that only ever forwards.
    implementation(project(":core:datastore"))
}
