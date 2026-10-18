plugins {
    id("com.example.wp04.dlplay.android.feature")
}

android {
    namespace = "com.example.wp04.dlplay.feature.onboarding"
}

dependencies {
    // The completion flag is a preference, not a domain fact — there is no repository worth
    // putting in front of one boolean that only this screen writes.
    implementation(project(":core:datastore"))
}
