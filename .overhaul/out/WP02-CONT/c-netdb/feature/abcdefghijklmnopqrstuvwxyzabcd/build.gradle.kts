plugins {
    id("io.example.fitcnetdb.android.feature")
}

android {
    namespace = "io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvwxyzabcd"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyzabcd"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyzabcd")))
}
