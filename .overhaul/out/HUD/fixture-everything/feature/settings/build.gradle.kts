plugins {
    id("com.example.hud.fixture.android.feature")
}

android {
    namespace = "com.example.hud.fixture.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))
}
