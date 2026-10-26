plugins {
    id("com.example.finalafter.crui.android.library")
    id("com.example.finalafter.crui.android.compose")
    id("com.example.finalafter.crui.android.hilt")
    // AppMessagingService is an @AndroidEntryPoint, which the Hilt plugin rewrites.
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.finalafter.crui.core.notification"

    // The channel and notification tests read this module's strings under Robolectric, which
    // needs the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
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
