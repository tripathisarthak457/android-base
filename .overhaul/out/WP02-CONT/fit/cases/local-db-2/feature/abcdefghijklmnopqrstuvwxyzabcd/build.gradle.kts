plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyzabcd"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyzabcd")))
}
