plugins {
    id("com.example.finalafter.crnoui.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui.feature.search"
}

dependencies {
    implementation(project(":data:search"))

    testImplementation(testFixtures(project(":data:search")))
}
