plugins {
    id("com.example.hud.everything.android.feature")
}

android {
    namespace = "com.example.hud.everything.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))
}
