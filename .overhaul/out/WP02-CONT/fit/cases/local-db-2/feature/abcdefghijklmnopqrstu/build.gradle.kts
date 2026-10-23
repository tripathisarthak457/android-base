plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstu"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstu"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstu")))
}
