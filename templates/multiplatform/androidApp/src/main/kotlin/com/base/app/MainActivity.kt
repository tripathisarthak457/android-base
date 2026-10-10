package com.base.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.base.app.shared.App

/** The one Activity: it hands the window to the shared UI. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The tab bar draws its own surface; the system's translucent scrim behind it would not match.
            window.isNavigationBarContrastEnforced = false
        }
        setContent { App() }
    }
}
