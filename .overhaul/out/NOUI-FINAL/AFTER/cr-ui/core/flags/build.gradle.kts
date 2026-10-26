plugins {
    id("com.example.finalafter.crui.android.library")
    id("com.example.finalafter.crui.android.hilt")
}

android {
    namespace = "com.example.finalafter.crui.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(project(":core:testing"))
}
