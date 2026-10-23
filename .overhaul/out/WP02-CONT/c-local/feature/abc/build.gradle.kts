plugins {
    id("io.example.fitclocal.android.feature")
}

android {
    namespace = "io.example.fitclocal.feature.abc"
}

dependencies {
    implementation(project(":data:abc"))

    testImplementation(testFixtures(project(":data:abc")))
}
