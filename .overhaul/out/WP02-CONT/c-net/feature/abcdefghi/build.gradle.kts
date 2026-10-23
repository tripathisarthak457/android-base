plugins {
    id("io.example.fitcnet.android.feature")
}

android {
    namespace = "io.example.fitcnet.feature.abcdefghi"
}

dependencies {
    implementation(project(":data:abcdefghi"))

    testImplementation(testFixtures(project(":data:abcdefghi")))
}
