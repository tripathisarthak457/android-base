plugins {
    id("io.example.fitclocal.android.feature")
}

android {
    namespace = "io.example.fitclocal.feature.abcdefghijklmnopqrstuvw"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvw"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvw")))
}
