plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.wp04.base.core.notification"
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
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(project(":core:testing"))
}
