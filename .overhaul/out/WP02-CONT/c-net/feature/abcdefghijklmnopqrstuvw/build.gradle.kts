plugins {
    id("io.example.fitcnet.android.feature")
}

android {
    namespace = "io.example.fitcnet.feature.abcdefghijklmnopqrstuvw"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvw"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvw")))
}
