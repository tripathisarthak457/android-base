package com.base.app.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The catalog: every component, every state, on a device.
 *
 * Its own application rather than a screen inside the app. It installs beside the real app so a
 * designer or a tester can hold both at once, it ships nothing into production, and it depends on
 * `:core:designsystem` alone — so iterating on a component rebuilds two modules rather than the
 * whole graph. That last point is the one you feel: it turns a design tweak from a ninety-second
 * cycle into a five-second one.
 */
class CatalogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CatalogApp() }
    }
}
