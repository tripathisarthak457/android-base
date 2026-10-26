plugins {
    id("com.example.finalafter.crui.android.feature")
}

android {
    namespace = "com.example.finalafter.crui.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))

    testImplementation(testFixtures(project(":core:datastore")))
}
