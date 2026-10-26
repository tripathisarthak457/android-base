plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.onboarding"
}

dependencies {
    // The completion flag is a preference, not a domain fact — there is no repository worth
    // putting in front of one boolean that only this screen writes.
    implementation(project(":core:datastore"))
    implementation(project(":core:notification"))

    testImplementation(testFixtures(project(":core:datastore")))
}
