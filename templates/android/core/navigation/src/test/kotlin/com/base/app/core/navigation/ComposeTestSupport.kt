package com.base.app.core.navigation

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag

internal fun ComposeTestRule.onNodeWithTagExists(key: AppNavKey) {
    onNodeWithTag(key.toString(), useUnmergedTree = true).assertExists()
}
