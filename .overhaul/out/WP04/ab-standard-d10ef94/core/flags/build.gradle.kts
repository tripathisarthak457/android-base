plugins {
    id("com.example.wp04.abstd.android.library")
    id("com.example.wp04.abstd.android.hilt")
}

android {
    namespace = "com.example.wp04.abstd.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
}
