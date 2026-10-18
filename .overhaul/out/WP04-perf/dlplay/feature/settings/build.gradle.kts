plugins {
    id("com.example.wp04.dlplay.android.feature")
}

android {
    namespace = "com.example.wp04.dlplay.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
}
