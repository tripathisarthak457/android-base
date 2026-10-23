plugins {
    id("com.example.standard.android.feature")
}

android {
    namespace = "com.example.standard.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
}
