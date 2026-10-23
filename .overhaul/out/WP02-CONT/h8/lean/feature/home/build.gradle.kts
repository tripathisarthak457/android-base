plugins {
    id("com.example.lean.android.feature")
}

android {
    namespace = "com.example.lean.feature.home"
}

dependencies {
    implementation(project(":data:home"))

    testImplementation(testFixtures(project(":data:home")))
}
