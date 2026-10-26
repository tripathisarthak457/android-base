plugins {
    id("com.example.finalafter.crui2.android.library")
    id("com.example.finalafter.crui2.android.hilt")
}

android {
    namespace = "com.example.finalafter.crui2.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(project(":core:testing"))
}
