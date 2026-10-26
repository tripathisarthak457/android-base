plugins {
    id("com.example.finalafter.crnoui.android.library")
    id("com.example.finalafter.crnoui.android.hilt")
}

android {
    namespace = "com.example.finalafter.crnoui.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(project(":core:testing"))
}
