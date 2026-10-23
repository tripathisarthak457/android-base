plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abc"
}

dependencies {
    implementation(project(":data:abc"))

    testImplementation(testFixtures(project(":data:abc")))
}
