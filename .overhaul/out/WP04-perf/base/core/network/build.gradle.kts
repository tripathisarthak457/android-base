plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.wp04.base.android.room")
}

android {
    namespace = "com.example.wp04.base.core.network"
}

dependencies {
    implementation(project(":core:common"))

    // The recorder writes into the inspector's log. `implementation` rather than `api`, so
    // the design system this pulls in behind it stays out of this module's own API.
    implementation(project(":core:devtools"))
    implementation(project(":core:coroutines"))
    api(project(":core:datastore"))

    api(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.websockets)

    api(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
}
