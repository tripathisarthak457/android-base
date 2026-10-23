plugins {
    id("com.example.standard.android.library")
    id("com.example.standard.android.hilt")
}

android {
    namespace = "com.example.standard.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
}
