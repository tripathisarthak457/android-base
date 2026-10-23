plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklm"
}

dependencies {
    implementation(project(":data:abcdefghijklm"))

    testImplementation(testFixtures(project(":data:abcdefghijklm")))
}
