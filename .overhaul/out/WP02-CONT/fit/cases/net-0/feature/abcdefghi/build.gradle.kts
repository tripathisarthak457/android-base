plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghi"
}

dependencies {
    implementation(project(":data:abcdefghi"))

    testImplementation(testFixtures(project(":data:abcdefghi")))
}
