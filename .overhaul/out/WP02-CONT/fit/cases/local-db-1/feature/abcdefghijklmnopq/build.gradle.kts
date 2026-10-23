plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopq"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopq"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopq")))
}
