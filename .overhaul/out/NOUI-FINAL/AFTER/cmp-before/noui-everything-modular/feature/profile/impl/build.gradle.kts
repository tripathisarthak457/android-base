plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.profile"
}

dependencies {
    api(project(":feature:profile:api"))
    implementation(project(":data:profile:api"))
    implementation(project(":core:media"))

    testImplementation(testFixtures(project(":data:profile:api")))
}
