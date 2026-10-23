package com.base.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.base.app.shared.App

/** The one Activity: it hands the window to the shared UI. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App(onDarkThemeChange = ::drawSystemBarsFor) }
    }

    /**
     * Light or dark system bar icons for the app's own theme: a person who picks Dark in settings on
     * a light phone would otherwise get dark icons on a dark app.
     */
    private fun drawSystemBarsFor(dark: Boolean) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // enableEdgeToEdge turns the contrast scrim back on; the tab bar draws its own surface.
            window.isNavigationBarContrastEnforced = false
        }
    }
}
