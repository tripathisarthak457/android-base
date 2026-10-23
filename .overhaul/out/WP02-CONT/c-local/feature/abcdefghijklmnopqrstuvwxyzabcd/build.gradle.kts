plugins {
    id("io.example.fitclocal.android.feature")
}

android {
    namespace = "io.example.fitclocal.feature.abcdefghijklmnopqrstuvwxyzabcd"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyzabcd"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyzabcd")))
}
