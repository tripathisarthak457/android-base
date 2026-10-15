package com.base.app.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.lifecycle.lifecycleScope
import com.base.app.core.designsystem.theme.AppFontPreload
import kotlinx.coroutines.launch

/**
 * The catalog: every component, every state, on a device. `--es section Buttons` opens a page
 * directly; see [CatalogEnvironment] for the theme, style and accessibility extras.
 */
class CatalogActivity : ComponentActivity() {

    private lateinit var frames: FrameTimes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        frames = FrameTimes(this)
        val section = CatalogSection.entries.firstOrNull { it.name == intent.getStringExtra("section") }
        val environment = CatalogEnvironment.from(intent)
        // The fonts arrive before the first frame or not at all this launch, as in the app, so no
        // text swaps typeface while it is on screen.
        lifecycleScope.launch {
            AppFontPreload.run(createFontFamilyResolver(this@CatalogActivity))
            setContent { CatalogApp(initialSection = section, initialEnvironment = environment, frames = frames) }
        }
    }

    override fun onDestroy() {
        frames.stop()
        super.onDestroy()
    }
}
