plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyzabc"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyzabc")))
}
