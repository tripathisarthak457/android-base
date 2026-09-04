plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.base.app.core.notification"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:coroutines"))
    implementation(project(":core:datastore"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    // The google-services plugin is applied by :app alone — it is what reads
    // google-services.json. The SDK itself is needed here, where the messaging service lives.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(project(":core:testing"))
}
