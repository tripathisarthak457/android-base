plugins {
    id("com.example.standardmodular.android.feature")
}

android {
    namespace = "com.example.standardmodular.feature.onboarding"
}

dependencies {
    api(project(":feature:onboarding:api"))
    // The completion flag is a preference, not a domain fact — there is no repository worth
    // putting in front of one boolean that only this screen writes.
    implementation(project(":core:datastore"))
}
