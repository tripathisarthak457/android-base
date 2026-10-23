plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrs"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrs"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrs")))
}
