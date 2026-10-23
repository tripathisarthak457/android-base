plugins {
    id("io.example.fitclocal.android.library")
    id("io.example.fitclocal.android.compose")
    id("io.example.fitclocal.android.screenshots")
}

android {
    namespace = "io.example.fitclocal.core.designsystem"
}

kotlin {
    compilerOptions {
        // The shell seams are this module's own API; only other modules have to opt in to them.
        optIn.add("io.example.fitclocal.core.designsystem.foundation.layout.ShellSeamApi")
    }
}

dependencies {

    // `api` so modules can use Modifier, Color and friends without re-declaring Compose.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.animation)

    // Android's own window size classes, so breakpoints match every other app on the device.
    implementation(libs.androidx.window.core)

    // Back and predictive back for sheets, alerts and the image viewer, and the lifecycle a toast's
    // time is counted against. Both are already in every app this module ships in.
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // A test activity that owns a back dispatcher, as every app's activity does.
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.robolectric)
}
