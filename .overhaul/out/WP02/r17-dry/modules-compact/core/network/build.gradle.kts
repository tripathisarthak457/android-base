plugins {
    id("com.example.modulescompact.android.library")
    id("com.example.modulescompact.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.modulescompact.core.network"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)

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
