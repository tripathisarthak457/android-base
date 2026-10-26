plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.settings"
}

dependencies {
    api(project(":feature:settings:api"))
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))

    testImplementation(testFixtures(project(":core:datastore")))
}
