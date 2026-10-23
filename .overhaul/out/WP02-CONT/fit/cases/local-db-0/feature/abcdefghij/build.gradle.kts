plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghij"
}

dependencies {
    implementation(project(":data:abcdefghij"))

    testImplementation(testFixtures(project(":data:abcdefghij")))
}
