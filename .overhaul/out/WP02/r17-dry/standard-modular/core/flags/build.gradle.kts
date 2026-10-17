plugins {
    id("com.example.standardmodular.android.library")
    id("com.example.standardmodular.android.hilt")
}

android {
    namespace = "com.example.standardmodular.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
}
