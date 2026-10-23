plugins {
    id("io.example.fitclocal.android.feature")
}

android {
    namespace = "io.example.fitclocal.feature.abcdefghijklmnopqrs"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrs"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrs")))
}
