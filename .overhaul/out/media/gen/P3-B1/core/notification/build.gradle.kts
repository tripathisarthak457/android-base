plugins {
    id("com.example.everything.android.library")
    id("com.example.everything.android.compose")
    id("com.example.everything.android.hilt")
    // AppMessagingService is an @AndroidEntryPoint, which the Hilt plugin rewrites.
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.everything.core.notification"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Messaging and Installations only: push needs nothing else from Firebase. The google-services
    // plugin, which reads google-services.json, is applied by :app.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
}
