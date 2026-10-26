plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.home"
}

dependencies {
    implementation(project(":data:home"))

    testImplementation(testFixtures(project(":data:home")))
}
