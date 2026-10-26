plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))

    testImplementation(testFixtures(project(":data:feed")))
}
