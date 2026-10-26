plugins {
    id("com.example.finalafter.crui.android.feature")
}

android {
    namespace = "com.example.finalafter.crui.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))

    testImplementation(testFixtures(project(":data:profile")))
}
