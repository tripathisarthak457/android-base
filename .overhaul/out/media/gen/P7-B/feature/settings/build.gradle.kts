plugins {
    id("com.example.everything.android.feature")
}

android {
    namespace = "com.example.everything.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))
}
