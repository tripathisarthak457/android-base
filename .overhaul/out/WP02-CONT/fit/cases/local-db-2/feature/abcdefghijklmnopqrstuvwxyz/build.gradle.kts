plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvwxyz"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyz"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyz")))
}
