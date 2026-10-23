plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmnopqrstuv"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuv"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuv")))
}
