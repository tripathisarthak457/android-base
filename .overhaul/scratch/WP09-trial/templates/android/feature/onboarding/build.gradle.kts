plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.onboarding"
}

dependencies {
    // The completion flag is a preference, not a domain fact — there is no repository worth
    // putting in front of one boolean that only this screen writes.
    implementation(project(":core:datastore"))
}
