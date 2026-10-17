plugins {
    id("com.example.modulescompact.android.feature")
}

android {
    namespace = "com.example.modulescompact.feature.wallet"
}

dependencies {
    implementation(project(":data:wallet"))

    testImplementation(testFixtures(project(":data:wallet")))
}
