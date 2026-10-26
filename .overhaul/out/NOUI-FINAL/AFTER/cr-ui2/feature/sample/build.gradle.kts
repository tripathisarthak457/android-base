plugins {
    id("com.example.finalafter.crui2.android.feature")
}

android {
    namespace = "com.example.finalafter.crui2.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))

    testImplementation(testFixtures(project(":data:sample")))
}
