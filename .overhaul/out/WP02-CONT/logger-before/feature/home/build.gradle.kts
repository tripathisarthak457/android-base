plugins {
    id("com.example.none.android.feature")
}

android {
    namespace = "com.example.none.feature.home"
}

dependencies {
    implementation(project(":data:home"))

    testImplementation(testFixtures(project(":data:home")))
}
