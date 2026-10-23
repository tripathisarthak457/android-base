plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefg"
}

dependencies {
    implementation(project(":data:abcdefg"))

    testImplementation(testFixtures(project(":data:abcdefg")))
}
