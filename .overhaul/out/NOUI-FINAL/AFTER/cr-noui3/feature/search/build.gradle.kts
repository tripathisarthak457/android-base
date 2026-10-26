plugins {
    id("com.example.finalafter.crnoui3.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui3.feature.search"
}

dependencies {
    implementation(project(":data:search"))

    testImplementation(testFixtures(project(":data:search")))
}
