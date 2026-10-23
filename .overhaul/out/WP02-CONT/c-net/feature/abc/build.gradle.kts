plugins {
    id("io.example.fitcnet.android.feature")
}

android {
    namespace = "io.example.fitcnet.feature.abc"
}

dependencies {
    implementation(project(":data:abc"))

    testImplementation(testFixtures(project(":data:abc")))
}
