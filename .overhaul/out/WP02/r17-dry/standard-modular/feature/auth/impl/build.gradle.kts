plugins {
    id("com.example.standardmodular.android.feature")
}

android {
    namespace = "com.example.standardmodular.feature.auth"
}

dependencies {
    api(project(":feature:auth:api"))
    implementation(project(":data:auth:api"))

    // A real Bundle, for the process-death test of the sign-in form.
    testImplementation(libs.robolectric)
}
