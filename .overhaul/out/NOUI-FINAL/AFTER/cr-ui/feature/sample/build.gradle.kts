plugins {
    id("com.example.finalafter.crui.android.feature")
}

android {
    namespace = "com.example.finalafter.crui.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))

    testImplementation(testFixtures(project(":data:sample")))
}
