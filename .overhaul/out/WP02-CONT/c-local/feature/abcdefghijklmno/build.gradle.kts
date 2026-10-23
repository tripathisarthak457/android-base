plugins {
    id("io.example.fitclocal.android.feature")
}

android {
    namespace = "io.example.fitclocal.feature.abcdefghijklmno"
}

dependencies {
    implementation(project(":data:abcdefghijklmno"))

    testImplementation(testFixtures(project(":data:abcdefghijklmno")))
}
