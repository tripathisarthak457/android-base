plugins {
    id("io.example.fitcnet.android.feature")
}

android {
    namespace = "io.example.fitcnet.feature.abcdefghijklmno"
}

dependencies {
    implementation(project(":data:abcdefghijklmno"))

    testImplementation(testFixtures(project(":data:abcdefghijklmno")))
}
