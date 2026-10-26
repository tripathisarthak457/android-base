plugins {
    id("com.example.finalafter.crnoui3.android.data")
}

android {
    namespace = "com.example.finalafter.crnoui3.data.auth"
}

dependencies {
    implementation(project(":core:network"))
    // Signing in writes the token store directly; a repository in front would only forward.
    implementation(project(":core:datastore"))

    // The token store in memory, for the repository's tests.
    testImplementation(testFixtures(project(":core:datastore")))
}
