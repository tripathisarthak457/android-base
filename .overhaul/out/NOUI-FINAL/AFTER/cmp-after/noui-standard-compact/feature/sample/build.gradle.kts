plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))

    testImplementation(testFixtures(project(":data:sample")))
}
