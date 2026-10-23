plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.a"
}

dependencies {
    implementation(project(":data:a"))

    testImplementation(testFixtures(project(":data:a")))
}
