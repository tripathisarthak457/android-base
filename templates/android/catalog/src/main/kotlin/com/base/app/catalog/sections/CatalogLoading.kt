package com.base.app.catalog.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.catalog.catalogContentPadding
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.feedback.AppRedacted
import com.base.app.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.base.app.core.designsystem.component.navigation.scrolledFromTop
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A measurement fixture for the shimmer: a list of real rows under a large title, redacted while
 * "loading", with one band crossing every row. "Load" lifts the redaction in place, on the same list
 * and data, so the phone measurements can watch the swap for movement: no row should move. Opened
 * directly with `--es section Loading`. An app does not build a loading screen this way: it shows a
 * separate placeholder list, with a list state of its own, as the feature screens do.
 */
@Composable
fun LoadingScreen(onBack: (() -> Unit)?) {
    var loaded by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    AppScaffold(
        topBar = {
            AppLargeTitleTopBar(
                title = "Inbox",
                subtitle = if (loaded) "12 conversations" else "Loading",
                scrolled = { list.scrolledFromTop() },
                onBack = onBack,
                actions = {
                    AppButton(
                        text = if (loaded) "Reload" else "Load",
                        onClick = { loaded = !loaded },
                        variant = ButtonVariant.Plain,
                        size = ButtonSize.Small,
                    )
                },
            )
        },
    ) {
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .testTag("loading_list")
                .then(if (loaded) Modifier else Modifier.clearAndSetSemantics { contentDescription = "Loading" }),
            contentPadding = catalogContentPadding(PaddingValues(AppTheme.spacing.gutter)),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            userScrollEnabled = loaded,
        ) {
            items(Conversations, key = Conversation::sender) { conversation ->
                AppRedacted(active = !loaded) { ConversationRow(conversation) }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation) {
    AppCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppAvatar(name = conversation.sender)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = conversation.sender,
                        modifier = Modifier.weight(1f),
                        style = AppTheme.typography.headline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AppText(
                        text = conversation.time,
                        style = AppTheme.typography.caption1,
                        color = AppTheme.colors.contentTertiary,
                    )
                }
                AppText(
                    text = conversation.preview,
                    style = AppTheme.typography.subheadline,
                    color = AppTheme.colors.contentSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Immutable
private class Conversation(val sender: String, val time: String, val preview: String)

// One-line and two-line previews mixed, as an inbox has them: each redacted row draws the bones of
// its own lines.
private val Conversations = listOf(
    Conversation("Maya Chen", "09:41", "Running ten minutes late. Start without me and I will catch up on the notes."),
    Conversation("Tomás Rivera", "09:12", "The slides are in the shared folder."),
    Conversation(
        "Priya Nair",
        "Yesterday",
        "Can we move Thursday's review to the afternoon? Two of us are out until noon.",
    ),
    Conversation("Jonas Berg", "Yesterday", "Approved, thanks."),
    Conversation(
        "Amara Okafor",
        "Mon",
        "I left a few comments on the draft, mostly about the second section and its numbers.",
    ),
    Conversation("Lena Fischer", "Mon", "The build is green again."),
    Conversation(
        "Kenji Sato",
        "Sun",
        "Photos from the offsite are up. The ones from the boat came out better than expected.",
    ),
    Conversation("Sofia Russo", "Sat", "Booked the room for Friday."),
    Conversation(
        "Omar Haddad",
        "Sat",
        "Here is the invoice for September. Let me know if anything looks off before I send it on.",
    ),
    Conversation("Hannah Weiss", "Fri", "Sounds good."),
    Conversation(
        "Diego Morales",
        "Thu",
        "Reminder that the office is closed on Monday for the holiday, back on Tuesday.",
    ),
    Conversation("Ines Laurent", "Wed", "Thanks for covering yesterday."),
)
