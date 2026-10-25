plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
    // AppMessagingService is an @AndroidEntryPoint, which the Hilt plugin rewrites.
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.base.app.core.notification"
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

    // The push store in memory, and a real NotificationManager and permission check under Robolectric.
    testImplementation(testFixtures(project(":core:datastore")))
    testImplementation(libs.robolectric)
}
