package com.example.finalafter.crui.core.navigation.shell

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.finalafter.crui.core.common.navigation.AppNavKey

/** The SDK the shell's Robolectric tests run on. */
internal const val ROBOLECTRIC_SDK = 35

internal fun ComposeTestRule.onNodeWithTagExists(key: AppNavKey) {
    onNodeWithTag(key.toString(), useUnmergedTree = true).assertExists()
}
