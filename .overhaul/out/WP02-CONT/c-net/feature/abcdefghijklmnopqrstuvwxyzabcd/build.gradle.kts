plugins {
    id("io.example.fitcnet.android.feature")
}

android {
    namespace = "io.example.fitcnet.feature.abcdefghijklmnopqrstuvwxyzabcd"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyzabcd"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyzabcd")))
}
