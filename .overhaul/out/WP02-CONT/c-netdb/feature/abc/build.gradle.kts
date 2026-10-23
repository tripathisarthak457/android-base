plugins {
    id("io.example.fitcnetdb.android.feature")
}

android {
    namespace = "io.example.fitcnetdb.feature.abc"
}

dependencies {
    implementation(project(":data:abc"))

    testImplementation(testFixtures(project(":data:abc")))
}
