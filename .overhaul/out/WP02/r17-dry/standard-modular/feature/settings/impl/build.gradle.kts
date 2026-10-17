plugins {
    id("com.example.standardmodular.android.feature")
}

android {
    namespace = "com.example.standardmodular.feature.settings"
}

dependencies {
    api(project(":feature:settings:api"))
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
}
