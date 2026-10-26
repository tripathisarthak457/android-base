plugins {
    id("com.example.finalafter.crui2.android.feature")
}

android {
    namespace = "com.example.finalafter.crui2.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))

    testImplementation(testFixtures(project(":data:feed")))
}
