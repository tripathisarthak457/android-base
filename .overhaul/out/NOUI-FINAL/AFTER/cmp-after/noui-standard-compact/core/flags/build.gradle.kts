plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.hilt")
}

android {
    namespace = "com.example.cmp.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
}
