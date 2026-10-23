package io.example.audit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.example.audit.core.designsystem.preview.AppScreenPreviews

// Planted by out/WP02-CONT/v4.sh to prove AdaptiveAuditTest fails on each kind of breakage; never committed.
@AppScreenPreviews
@Composable
private fun AuditPlantedPreview() {
    Box(Modifier.fillMaxSize()) {
        Column {
            BasicText(LONG, Modifier.height(10.dp).testTag("cut"))
            Row {
                Box(Modifier.size(24.dp).testTag("tiny-a").clickable {})
                Box(Modifier.size(24.dp).testTag("tiny-b").clickable {})
            }
            Box(Modifier.requiredWidth(3000.dp).height(48.dp).testTag("wide").clickable {})
            Box {
                Box(Modifier.size(60.dp).testTag("under").clickable {})
                Box(Modifier.offset(x = 30.dp).size(60.dp).testTag("over").clickable {})
            }
            BasicText(LONG + LONG + LONG, Modifier.fillMaxWidth().testTag("long-lines"))
        }
        Box(Modifier.align(Alignment.BottomCenter).size(200.dp, 56.dp).testTag("bottom").clickable {})
    }
}

private const val LONG =
    "A sentence long enough to run across several lines on a phone and across one very long line on a desktop window. "
