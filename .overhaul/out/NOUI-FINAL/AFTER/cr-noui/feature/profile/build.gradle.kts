plugins {
    id("com.example.finalafter.crnoui.android.feature")
}

android {
    namespace = "com.example.finalafter.crnoui.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))

    testImplementation(testFixtures(project(":data:profile")))
}
