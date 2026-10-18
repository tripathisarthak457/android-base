package com.example.wp04.abstd.core.navigation

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.wp04.abstd.core.common.navigation.AppNavKey

internal fun ComposeTestRule.onNodeWithTagExists(key: AppNavKey) {
    onNodeWithTag(key.toString(), useUnmergedTree = true).assertExists()
}
