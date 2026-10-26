package com.example.cmp.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.lifecycle.lifecycleScope
import com.example.cmp.core.designsystem.theme.AppFontPreload
import kotlinx.coroutines.launch

/**
 * The catalog: every component, every state, on a device. `--es section Buttons` opens a page
 * directly; see [CatalogEnvironment] for the theme, style and accessibility extras.
 */
class CatalogActivity : ComponentActivity() {

    private lateinit var frames: FrameTimes
    private var fontsDecided by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        frames = FrameTimes(this)
        val section = CatalogSection.entries.firstOrNull {
            it.name ==
                intent.getStringExtra("section")
        }
        val environment = CatalogEnvironment.from(intent)
        // The fonts arrive before the first frame or not at all this launch, as in the app, so no
        // text swaps typeface while it is on screen. Until they are decided the view reads the
        // window's insets: Compose asks the window for them only once a composition reads them,
        // and they arrive a frame later, so a catalog composed first would lay its bars out once
        // without the status bar and every page would jump by its height.
        setContent {
            if (fontsDecided) {
                CatalogApp(
                    initialSection = section,
                    initialEnvironment = environment,
                    frames = frames,
                )
            } else {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }
        }
        lifecycleScope.launch {
            AppFontPreload.run(createFontFamilyResolver(this@CatalogActivity))
            fontsDecided = true
        }
    }

    override fun onDestroy() {
        frames.stop()
        super.onDestroy()
    }
}
