plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.search"
}

dependencies {
    implementation(project(":data:search"))

    testImplementation(testFixtures(project(":data:search")))
}
