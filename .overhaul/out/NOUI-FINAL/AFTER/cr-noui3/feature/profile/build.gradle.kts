plugins {
    id("com.example.finalafter.crnoui3.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui3.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))

    testImplementation(testFixtures(project(":data:profile")))
}
