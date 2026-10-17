plugins {
    id("com.example.standardmodular.android.library")
    id("com.example.standardmodular.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.standardmodular.android.room")
}

android {
    namespace = "com.example.standardmodular.core.network"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)

    // The recorder writes into the inspector's log. `implementation` rather than `api`, so
    // the design system this pulls in behind it stays out of this module's own API.
    implementation(project(":core:devtools"))
    api(project(":core:datastore"))

    api(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)

    api(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
}
