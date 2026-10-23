plugins {
    id("io.example.fitcnetdb.android.feature")
}

android {
    namespace = "io.example.fitcnetdb.feature.abcdefghijklmnopqrstuvw"
}

dependencies {
    implementation(project(":data:abcdefghijklmnopqrstuvw"))

    testImplementation(testFixtures(project(":data:abcdefghijklmnopqrstuvw")))
}
