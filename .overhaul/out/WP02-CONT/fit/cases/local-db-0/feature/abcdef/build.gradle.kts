plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdef"
}

dependencies {
    implementation(project(":data:abcdef"))

    testImplementation(testFixtures(project(":data:abcdef")))
}
