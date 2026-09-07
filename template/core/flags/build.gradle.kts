plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    // <opt:flags-remote>
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    // </opt:flags-remote>

    testImplementation(project(":core:testing"))
}
