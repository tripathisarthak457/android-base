plugins {
    id("com.example.finalafter.crui2.android.data")
}

android {
    namespace = "com.example.finalafter.crui2.data.auth"
}

dependencies {
    implementation(project(":core:network"))
    // Signing in writes the token store directly; a repository in front would only forward.
    implementation(project(":core:datastore"))

    // The token store in memory, for the repository's tests.
    testImplementation(testFixtures(project(":core:datastore")))
}
