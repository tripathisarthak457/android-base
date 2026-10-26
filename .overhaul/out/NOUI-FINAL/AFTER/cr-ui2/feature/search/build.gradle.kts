plugins {
    id("com.example.finalafter.crui2.android.feature")
}

android {
    namespace = "com.example.finalafter.crui2.feature.search"
}

dependencies {
    implementation(project(":data:search"))

    testImplementation(testFixtures(project(":data:search")))
}
