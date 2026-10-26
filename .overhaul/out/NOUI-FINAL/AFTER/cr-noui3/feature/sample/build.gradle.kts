plugins {
    id("com.example.finalafter.crnoui3.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui3.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))

    testImplementation(testFixtures(project(":data:sample")))
}
