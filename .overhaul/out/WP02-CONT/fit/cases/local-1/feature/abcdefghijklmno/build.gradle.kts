plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmno"
}

dependencies {
    implementation(project(":data:abcdefghijklmno"))

    testImplementation(testFixtures(project(":data:abcdefghijklmno")))
}
