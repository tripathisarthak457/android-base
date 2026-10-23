plugins {
    id("com.example.videoalone.android.feature")
}

android {
    namespace = "com.example.videoalone.feature.home"
}

dependencies {
    implementation(project(":data:home"))

    testImplementation(testFixtures(project(":data:home")))
}
