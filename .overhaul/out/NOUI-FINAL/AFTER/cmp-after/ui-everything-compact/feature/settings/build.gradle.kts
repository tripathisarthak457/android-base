plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))

    testImplementation(testFixtures(project(":core:datastore")))
}
