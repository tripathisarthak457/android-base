plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    // <opt:room>
    id("com.base.app.android.room")
    // </opt:room>
}

android {
    namespace = "com.base.app.core.network"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:coroutines"))
    api(project(":core:datastore"))

    api(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    // <opt:websocket>
    implementation(libs.ktor.client.websockets)
    // </opt:websocket>

    api(libs.kotlinx.serialization.json)

    // <opt:workmanager>
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // </opt:workmanager>

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
}
