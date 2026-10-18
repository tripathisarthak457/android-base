plugins {
    id("com.example.wp04.dlplay.android.library")
    id("com.example.wp04.dlplay.android.hilt")
}

android {
    namespace = "com.example.wp04.dlplay.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(project(":core:testing"))
}
