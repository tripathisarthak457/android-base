plugins {
    id("io.example.audit.android.feature")
}

android {
    namespace = "io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvwxyzabcd"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvwxyzabcd")))
}
