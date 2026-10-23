plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrst"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrst"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrst")))
}
