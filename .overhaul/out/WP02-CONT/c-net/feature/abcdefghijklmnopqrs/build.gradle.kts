plugins {
    id("io.example.fitcnet.android.feature")
}

android {
    namespace = "io.example.fitcnet.feature.abcdefghijklmnopqrs"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrs"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrs")))
}
