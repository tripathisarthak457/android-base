plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuvw"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvw"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvw")))
}
