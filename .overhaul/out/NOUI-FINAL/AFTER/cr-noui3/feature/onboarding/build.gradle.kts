plugins {
    id("com.example.finalafter.crnoui3.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui3.feature.onboarding"
}

dependencies {
    // The completion flag is a preference, not a domain fact — there is no repository worth
    // putting in front of one boolean that only this screen writes.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))

    testImplementation(testFixtures(project(":core:datastore")))
}
