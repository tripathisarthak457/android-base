plugins {
    id("io.example.fitcnetdb.android.feature")
}

android {
    namespace = "io.example.fitcnetdb.feature.abcdefghijklmnopqrs"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrs"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrs")))
}
