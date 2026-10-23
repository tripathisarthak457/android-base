plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvwxy"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxy"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxy")))
}
