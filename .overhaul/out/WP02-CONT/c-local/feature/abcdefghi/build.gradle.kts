plugins {
    id("io.example.fitclocal.android.feature")
}

android {
    namespace = "io.example.fitclocal.feature.abcdefghi"
}

dependencies {
    implementation(project(":data:abcdefghi"))

    testImplementation(testFixtures(project(":data:abcdefghi")))
}
