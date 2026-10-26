plugins {
    id("com.example.finalafter.crui.android.feature")
}

android {
    namespace = "com.example.finalafter.crui.feature.search"
}

dependencies {
    implementation(project(":data:search"))

    testImplementation(testFixtures(project(":data:search")))
}
