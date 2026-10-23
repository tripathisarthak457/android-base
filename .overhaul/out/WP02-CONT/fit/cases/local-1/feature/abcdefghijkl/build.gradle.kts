plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijkl"
}

dependencies {
    implementation(project(":data:abcdefghijkl"))

    testImplementation(testFixtures(project(":data:abcdefghijkl")))
}
