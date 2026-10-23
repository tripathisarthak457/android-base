plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnop"
}

dependencies {
    implementation(project(":data:abcdefghijklmnop"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnop")))
}
