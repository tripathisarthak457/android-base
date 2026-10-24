plugins {
    id("com.example.deskstandard.android.feature")
}

android {
    namespace = "com.example.deskstandard.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
}
