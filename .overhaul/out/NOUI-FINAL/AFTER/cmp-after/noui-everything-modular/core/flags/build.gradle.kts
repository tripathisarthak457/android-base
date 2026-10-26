plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.hilt")
}

android {
    namespace = "com.example.cmp.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(project(":core:testing"))
}
