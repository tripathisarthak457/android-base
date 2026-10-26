plugins {
    id("com.example.finalafter.crui2.android.feature")
}

android {
    namespace = "com.example.finalafter.crui2.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))

    testImplementation(testFixtures(project(":data:profile")))
}
