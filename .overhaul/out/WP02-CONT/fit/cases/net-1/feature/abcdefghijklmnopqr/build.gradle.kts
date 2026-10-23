plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqr"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqr"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqr")))
}
