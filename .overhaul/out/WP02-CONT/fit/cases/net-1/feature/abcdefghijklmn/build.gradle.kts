plugins {
    id("io.example.fit.android.feature")
}

android {
    namespace = "io.example.fit.feature.abcdefghijklmn"
}

dependencies {
    implementation(project(":data:abcdefghijklmn"))

    testImplementation(testFixtures(project(":data:abcdefghijklmn")))
}
