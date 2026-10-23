plugins {
    id("io.example.fitcnetdb.android.feature")
}

android {
    namespace = "io.example.fitcnetdb.feature.abcdefghi"
}

dependencies {
    implementation(project(":data:abcdefghi"))

    testImplementation(testFixtures(project(":data:abcdefghi")))
}
