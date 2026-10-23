plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcde"
}

dependencies {
    implementation(project(":data:abcde"))

    testImplementation(testFixtures(project(":data:abcde")))
}
