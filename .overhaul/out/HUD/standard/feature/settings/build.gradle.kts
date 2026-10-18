plugins {
    id("com.example.hud.standard.android.feature")
}

android {
    namespace = "com.example.hud.standard.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
}
