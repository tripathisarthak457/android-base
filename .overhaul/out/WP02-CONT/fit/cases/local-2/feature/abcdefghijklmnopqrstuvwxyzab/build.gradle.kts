plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyzab"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyzab")))
}
