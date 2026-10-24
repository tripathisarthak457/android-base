plugins {
    id("com.example.deskstandard.android.library")
    id("com.example.deskstandard.android.hilt")
}

android {
    namespace = "com.example.deskstandard.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
}
