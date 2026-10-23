plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcd"
}

dependencies {
    implementation(project(":data:abcd"))

    testImplementation(testFixtures(project(":data:abcd")))
}
