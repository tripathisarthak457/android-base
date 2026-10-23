plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijk"
}

dependencies {
    implementation(project(":data:abcdefghijk"))

    testImplementation(testFixtures(project(":data:abcdefghijk")))
}
