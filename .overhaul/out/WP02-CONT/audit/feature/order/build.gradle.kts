plugins {
    id("io.example.audit.android.feature")
}

android {
    namespace = "io.example.audit.feature.order"
}

dependencies {
    implementation(project(":data:order"))

    testImplementation(testFixtures(project(":data:order")))
}
