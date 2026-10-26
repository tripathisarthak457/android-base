plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))

    testImplementation(testFixtures(project(":data:profile")))
}
