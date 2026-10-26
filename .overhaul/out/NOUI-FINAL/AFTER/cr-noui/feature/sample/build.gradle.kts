plugins {
    id("com.example.finalafter.crnoui.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))

    testImplementation(testFixtures(project(":data:sample")))
}
