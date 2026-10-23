plugins {
    id("io.example.fitcnetdb.android.feature")
}

android {
    namespace = "io.example.fitcnetdb.feature.abcdefghijklmno"
}

dependencies {
    implementation(project(":data:abcdefghijklmno"))

    testImplementation(testFixtures(project(":data:abcdefghijklmno")))
}
