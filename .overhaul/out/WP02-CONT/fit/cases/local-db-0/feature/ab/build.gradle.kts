plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.ab"
}

dependencies {
    implementation(project(":data:ab"))

    testImplementation(testFixtures(project(":data:ab")))
}
