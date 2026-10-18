plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    // <opt:firebase>
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    // </opt:firebase>

    testImplementation(project(":core:testing"))
}
