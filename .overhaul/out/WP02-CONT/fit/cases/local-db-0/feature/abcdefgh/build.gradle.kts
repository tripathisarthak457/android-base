plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefgh"
}

dependencies {
    implementation(project(":data:abcdefgh"))

    testImplementation(testFixtures(project(":data:abcdefgh")))
}
