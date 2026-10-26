plugins {
    id("com.example.finalafter.crui.android.feature")
}

android {
    namespace = "com.example.finalafter.crui.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))

    testImplementation(testFixtures(project(":data:feed")))
}
