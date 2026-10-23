plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvwx"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwx"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwx")))
}
