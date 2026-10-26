plugins {
    id("com.example.finalafter.crnoui3.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui3.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))

    testImplementation(testFixtures(project(":data:feed")))
}
