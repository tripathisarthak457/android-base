plugins {
    id("com.example.finalafter.crnoui.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))

    testImplementation(testFixtures(project(":data:feed")))
}
