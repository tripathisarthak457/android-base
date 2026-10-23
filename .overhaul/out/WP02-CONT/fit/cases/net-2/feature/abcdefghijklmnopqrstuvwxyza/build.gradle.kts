plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvwxyza"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyza"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyza")))
}
