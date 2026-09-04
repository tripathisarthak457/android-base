package com.base.app.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.datetime.AppDatePicker
import com.base.app.core.designsystem.component.datetime.AppTimePicker
import com.base.app.core.designsystem.component.datetime.AppWheelPicker
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.feedback.AppBadgedBox
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppCircularProgress
import com.base.app.core.designsystem.component.feedback.AppDotBadge
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.component.feedback.AppErrorState
import com.base.app.core.designsystem.component.feedback.AppLinearProgress
import com.base.app.core.designsystem.component.feedback.AppSkeletonListItem
import com.base.app.core.designsystem.component.feedback.AppSnackbar
import com.base.app.core.designsystem.component.feedback.AppStatusPill
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.input.AppNumberField
import com.base.app.core.designsystem.component.input.AppPasswordField
import com.base.app.core.designsystem.component.input.AppSearchField
import com.base.app.core.designsystem.component.input.AppTextArea
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.component.navigation.AppBottomBar
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.navigation.AppTabRow
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.component.navigation.BottomNavItem
import com.base.app.core.designsystem.component.overlay.AppActionSheet
import com.base.app.core.designsystem.component.overlay.AppAlertDialog
import com.base.app.core.designsystem.component.overlay.AppBottomSheet
import com.base.app.core.designsystem.component.overlay.AppDropdownMenu
import com.base.app.core.designsystem.component.overlay.AppLoadingOverlay
import com.base.app.core.designsystem.component.overlay.AppMenuDivider
import com.base.app.core.designsystem.component.overlay.AppMenuItem
import com.base.app.core.designsystem.component.overlay.AppTooltip
import com.base.app.core.designsystem.component.overlay.SheetAction
import com.base.app.core.designsystem.component.selection.AppCheckbox
import com.base.app.core.designsystem.component.selection.AppChip
import com.base.app.core.designsystem.component.selection.AppRadioButton
import com.base.app.core.designsystem.component.selection.AppSegmentedControl
import com.base.app.core.designsystem.component.selection.AppSlider
import com.base.app.core.designsystem.component.selection.AppSwitch
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ButtonsSection() {
    var loading by remember { mutableStateOf(false) }

    CatalogGroup(title = "Variants", caption = "Pick by importance, not by colour.") {
        AppButton("Primary", {}, fillWidth = true)
        AppButton("Secondary", {}, variant = ButtonVariant.Secondary, fillWidth = true)
        AppButton("Tertiary", {}, variant = ButtonVariant.Tertiary, fillWidth = true)
        AppButton("Destructive", {}, variant = ButtonVariant.Destructive, fillWidth = true)
        AppButton("Ghost", {}, variant = ButtonVariant.Ghost, fillWidth = true)
    }

    CatalogGroup(title = "Sizes") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton("Small", {}, size = ButtonSize.Small)
            AppButton("Medium", {}, size = ButtonSize.Medium)
            AppButton("Large", {}, size = ButtonSize.Large)
        }
    }

    CatalogGroup(title = "With icons") {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppButton("Download", {}, leadingIcon = AppIcons.Download)
            AppButton("Next", {}, variant = ButtonVariant.Secondary, trailingIcon = AppIcons.ArrowRight)
        }
    }

    CatalogGroup(
        title = "Loading and disabled",
        caption = "The loading button keeps its width, and stops accepting taps.",
    ) {
        AppButton(
            text = "Tap to load for two seconds",
            onClick = { loading = true },
            loading = loading,
            fillWidth = true,
        )
        AppButton("Disabled", {}, enabled = false, fillWidth = true)
    }

    if (loading) {
        LaunchedEffect(Unit) {
            delay(LOADING_DEMO_MILLIS)
            loading = false
        }
    }

    CatalogGroup(title = "Icon buttons") {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppIconButton(AppIcons.Heart, "Favourite", {})
            AppIconButton(AppIcons.Share, "Share", {}, variant = ButtonVariant.Secondary)
            AppIconButton(AppIcons.Trash, "Delete", {}, variant = ButtonVariant.Destructive)
        }
    }
}

@Composable
fun InputsSection() {
    var text by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    CatalogGroup(title = "Text", caption = "Label above the field, never floating into the border.") {
        AppTextField(
            value = text,
            onValueChange = { text = it },
            label = "Full name",
            placeholder = "Ada Lovelace",
            helper = "As it appears on your ID.",
        )
        AppTextField(
            value = text,
            onValueChange = { text = it },
            label = "With an error",
            error = "This name is already taken.",
        )
        AppTextField(
            value = "Read only",
            onValueChange = {},
            label = "Disabled",
            enabled = false,
        )
    }

    CatalogGroup(title = "Specialised") {
        AppPasswordField(value = password, onValueChange = { password = it }, label = "Password")
        AppSearchField(value = search, onValueChange = { search = it })
        AppNumberField(
            value = number,
            onValueChange = { number = it },
            label = "Quantity",
            maxLength = 4,
        )
        AppTextArea(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes",
            maxLength = 200,
        )
    }
}

@Composable
fun SelectionSection() {
    var checked by remember { mutableStateOf(true) }
    var selected by remember { mutableIntStateOf(0) }
    var switched by remember { mutableStateOf(true) }
    var slider by remember { mutableFloatStateOf(0.4f) }
    var stepped by remember { mutableFloatStateOf(3f) }
    var segment by remember { mutableIntStateOf(0) }
    var chips by remember { mutableStateOf(setOf("Design")) }

    CatalogGroup(title = "Checkbox", caption = "The tick is drawn on, not faded in.") {
        AppCheckbox(checked, { checked = it }, label = "Send me updates")
        AppCheckbox(false, {}, label = "Unchecked")
        AppCheckbox(true, null, label = "Disabled", enabled = false)
    }

    CatalogGroup(title = "Radio") {
        listOf("Standard", "Express", "Pickup").forEachIndexed { index, option ->
            AppRadioButton(
                selected = selected == index,
                onClick = { selected = index },
                label = option,
            )
        }
    }

    CatalogGroup(title = "Switch") {
        AppSwitch(switched, { switched = it }, label = "Notifications")
        AppSwitch(false, {}, label = "Disabled", enabled = false)
    }

    CatalogGroup(title = "Slider", caption = "Tap the track to jump; the thumb haloes while dragged.") {
        AppSlider(value = slider, onValueChange = { slider = it })
        AppMonoText("%.2f".format(slider), color = AppTheme.colors.contentTertiary)
        AppSlider(value = stepped, onValueChange = { stepped = it }, valueRange = 0f..10f, steps = 9)
        AppMonoText("%.0f of 10".format(stepped), color = AppTheme.colors.contentTertiary)
    }

    CatalogGroup(title = "Segmented control") {
        AppSegmentedControl(
            options = listOf("All", "Active", "Archived"),
            selectedIndex = segment,
            onSelect = { segment = it },
        )
    }

    CatalogGroup(title = "Chips") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            listOf("Design", "Engineering", "Research", "Ops").forEach { label ->
                AppChip(
                    label = label,
                    selected = label in chips,
                    onClick = {
                        chips = if (label in chips) chips - label else chips + label
                    },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppChip("With icon", {}, leadingIcon = AppIcons.Filter)
            AppChip("Removable", {}, selected = true, onRemove = {})
        }
    }
}

@Composable
fun ContainersSection() {
    CatalogGroup(
        title = "Top bars",
        caption = "AppScaffold takes one of these; each already handles the status-bar inset.",
    ) {
        // Each bar applies `statusBarsPadding()`, which is right at the top of a window and wrong
        // half-way down this page — the demos would render 40dp taller than they really are.
        // Consuming the inset here tells them it has already been dealt with.
        Column(
            modifier = Modifier
                .consumeWindowInsets(WindowInsets.statusBars)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppTopBar(title = "Plain", subtitle = "With a subtitle")
            AppBackTopBar(
                title = "With a back arrow",
                onBack = {},
                actions = { AppIconButton(AppIcons.Search, "Search", {}) },
            )
            AppTopBar(
                title = "Centred",
                centerTitle = true,
                navigationIcon = AppIcons.Menu,
                navigationContentDescription = "Open the menu",
                onNavigationClick = {},
                actions = { AppIconButton(AppIcons.MoreVertical, "More", {}) },
            )
            AppLargeTitle(
                title = "Large title",
                subtitle = "For the root of a tab",
                actions = { AppIconButton(AppIcons.Filter, "Filter", {}) },
            )
        }
    }

    CatalogGroup(title = "Cards", caption = "Outlined by default; elevation is for things that float.") {
        AppCard {
            AppText("Outlined", style = AppTheme.typography.titleLarge)
            AppText(
                "The default. A list of eight elevated cards is eight competing shadows.",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentTertiary,
            )
        }
        AppCard(elevation = AppTheme.elevation.card, border = null) {
            AppText("Elevated", style = AppTheme.typography.titleLarge)
            AppText(
                "A shadow in light, a lighter surface with an outline in dark.",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentTertiary,
            )
        }
        AppCard(onClick = {}) {
            AppText("Clickable", style = AppTheme.typography.titleLarge)
            AppText(
                "No press scale — at this size it reads as the layout jumping.",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentTertiary,
            )
        }
    }

    CatalogGroup(title = "List rows") {
        AppListItem(
            title = "With everything",
            overline = "Overline",
            supporting = "Supporting copy on a second line.",
            onClick = {},
            leading = { AppAvatar(name = "Ada Lovelace", size = 40.dp) },
            trailing = {
                AppIcon(AppIcons.ChevronRight, null, tint = AppTheme.colors.contentTertiary)
            },
        )
        AppDivider(startIndent = 68.dp)
        AppListItem(
            title = "Title only",
            onClick = {},
            trailing = {
                AppIcon(AppIcons.ChevronRight, null, tint = AppTheme.colors.contentTertiary)
            },
        )
    }

    CatalogGroup(title = "Tabs") {
        var tab by remember { mutableIntStateOf(0) }
        AppTabRow(
            tabs = listOf("Overview", "Activity", "Settings"),
            selectedIndex = tab,
            onTabSelected = { tab = it },
        )
    }

    CatalogGroup(title = "Bottom bar") {
        var index by remember { mutableIntStateOf(0) }
        AppBottomBar(
            items = listOf(
                BottomNavItem("Home", AppIcons.Home),
                BottomNavItem("Search", AppIcons.Search),
                BottomNavItem("Alerts", AppIcons.Bell, badgeCount = 12),
                BottomNavItem("Profile", AppIcons.User),
            ),
            selectedIndex = index,
            onItemSelected = { index = it },
        )
    }
}

@Composable
fun FeedbackSection() {
    CatalogGroup(title = "Progress") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppCircularProgress()
            AppCircularProgress(size = 32.dp)
            AppCircularProgress(progress = 0.65f, size = 32.dp)
        }
        AppLinearProgress(modifier = Modifier.fillMaxWidth())
        AppLinearProgress(progress = 0.4f, modifier = Modifier.fillMaxWidth())
    }

    CatalogGroup(title = "Skeletons", caption = "Shaped like the content they stand in for.") {
        AppSkeletonListItem()
        AppSkeletonListItem()
    }

    CatalogGroup(title = "Badges and pills") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBadgedBox(count = 3) { AppIcon(AppIcons.Bell, "Notifications", size = 26.dp) }
            AppBadgedBox(count = 240) { AppIcon(AppIcons.Mail, "Inbox", size = 26.dp) }
            AppDotBadge(visible = true) { AppIcon(AppIcons.User, "Profile", size = 26.dp) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppStatusPill("Delivered", AppTheme.colors.success, icon = AppIcons.CheckCircle)
            AppStatusPill("Pending", AppTheme.colors.warning, icon = AppIcons.Clock)
            AppStatusPill("Failed", AppTheme.colors.danger, icon = AppIcons.XCircle)
            AppStatusPill("Draft", AppTheme.colors.neutral)
        }
    }

    CatalogGroup(title = "Avatars") {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            listOf("Ada Lovelace", "Grace Hopper", "Alan Turing", "Katherine Johnson").forEach {
                AppAvatar(name = it)
            }
        }
    }

    CatalogGroup(title = "Banners and snackbars") {
        AppBanner("You are offline. Showing saved data.", tone = AppTone.Warning)
        AppSnackbar(text = "Saved.", tone = AppTone.Success)
        AppSnackbar(
            text = "Could not reach the server.",
            title = "Upload failed",
            tone = AppTone.Error,
            actionLabel = "Retry",
            onAction = {},
        )
    }

    CatalogGroup(title = "Whole-screen states") {
        Box(modifier = Modifier.height(280.dp)) {
            AppEmptyState(
                title = "Nothing here yet",
                message = "When there is something to show, it appears on this screen.",
                icon = AppIcons.ListView,
                actionLabel = "Reload",
                onAction = {},
            )
        }
        Box(modifier = Modifier.height(280.dp)) {
            AppErrorState(message = "Check your connection and try again.", isOffline = true, onRetry = {})
        }
    }
}

@Composable
fun OverlaysSection() {
    var dialog by remember { mutableStateOf(false) }
    var destructive by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var actionSheet by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }

    if (overlay) {
        LaunchedEffect(Unit) {
            delay(OVERLAY_MILLIS)
            overlay = false
        }
    }

    CatalogGroup(title = "Dialogs", caption = "They scale in; there is no exit animation, by design.") {
        AppButton("Show dialog", { dialog = true }, fillWidth = true)
        AppButton(
            text = "Show destructive dialog",
            onClick = { destructive = true },
            variant = ButtonVariant.Secondary,
            fillWidth = true,
        )
    }

    CatalogGroup(title = "Bottom sheet", caption = "Drag it down — distance or velocity dismisses.") {
        AppButton("Show sheet", { sheet = true }, fillWidth = true)
    }

    CatalogGroup(title = "Dropdown menu") {
        Box {
            AppButton("Open menu", { menu = true }, variant = ButtonVariant.Secondary)
            AppDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                AppMenuItem("Edit", { menu = false }, icon = AppIcons.Edit)
                AppMenuItem("Duplicate", { menu = false }, icon = AppIcons.Copy)
                AppMenuItem("Share", { menu = false }, icon = AppIcons.Share)
                AppMenuDivider()
                AppMenuItem("Delete", { menu = false }, icon = AppIcons.Trash, destructive = true)
            }
        }
    }

    CatalogGroup(
        title = "Action sheet",
        caption = "A list of things to do with one item, with the dangerous one marked.",
    ) {
        AppButton("Show actions", { actionSheet = true }, variant = ButtonVariant.Secondary)
    }

    CatalogGroup(
        title = "Tooltip",
        caption = "Long-press the icon. It dismisses itself; there is nothing to tap away.",
    ) {
        AppTooltip(text = "Refreshes the list from the server") {
            AppIconButton(
                icon = AppIcons.Refresh,
                contentDescription = "Refresh",
                onClick = {},
            )
        }
    }

    CatalogGroup(
        title = "Loading overlay",
        caption = "For a blocking operation the user must wait out. Clears after a second.",
    ) {
        AppButton("Block the screen", { overlay = true }, fillWidth = true)
    }

    AppLoadingOverlay(visible = overlay, label = "Uploading…")

    if (actionSheet) {
        AppActionSheet(
            actions = listOf(
                SheetAction("Edit", { actionSheet = false }, icon = AppIcons.Edit),
                SheetAction("Duplicate", { actionSheet = false }, icon = AppIcons.Copy),
                SheetAction("Share", { actionSheet = false }, icon = AppIcons.Share),
                SheetAction(
                    label = "Delete",
                    onClick = { actionSheet = false },
                    icon = AppIcons.Trash,
                    isDestructive = true,
                ),
            ),
            onDismissRequest = { actionSheet = false },
            title = "Photo",
            message = "Taken 4 March, 09:41",
        )
    }

    if (dialog) {
        AppAlertDialog(
            title = "Save changes?",
            message = "Your edits will be kept and synced the next time you are online.",
            onDismissRequest = { dialog = false },
            confirmLabel = "Save",
            onConfirm = {},
            dismissLabel = "Cancel",
            icon = AppIcons.CheckCircle,
            tone = AppTone.Info,
        )
    }

    if (destructive) {
        AppAlertDialog(
            title = "Delete this item?",
            message = "This cannot be undone.",
            onDismissRequest = { destructive = false },
            confirmLabel = "Delete",
            onConfirm = {},
            dismissLabel = "Keep",
            icon = AppIcons.AlertTriangle,
            tone = AppTone.Error,
        )
    }

    if (sheet) {
        AppBottomSheet(onDismissRequest = { sheet = false }, title = "Sort by") {
            listOf("Newest first", "Oldest first", "A to Z").forEach { option ->
                AppListItem(title = option, onClick = { sheet = false })
            }
        }
    }
}

@Composable
fun DateTimeSection() {
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.of(9, 30)) }
    var wheel by remember { mutableIntStateOf(2) }

    CatalogGroup(
        title = "Calendar",
        caption = "The week starts where the device's locale says it does.",
    ) {
        AppDatePicker(selectedDate = date, onDateSelected = { date = it })
        AppMonoText(date?.toString().orEmpty(), color = AppTheme.colors.contentTertiary)
    }

    CatalogGroup(
        title = "Time",
        caption = "Twelve- or twenty-four-hour is read from the locale, not assumed.",
    ) {
        AppTimePicker(time = time, onTimeChange = { time = it }, minuteStep = 5)
        AppMonoText(time.toString(), color = AppTheme.colors.contentTertiary)
    }

    CatalogGroup(title = "Wheel") {
        AppWheelPicker(
            items = listOf("Never", "Daily", "Weekly", "Monthly", "Yearly"),
            selectedIndex = wheel,
            onSelectedChange = { wheel = it },
            label = { it },
        )
    }
}

@Composable
fun IconsSection() {
    CatalogGroup(
        title = "The set",
        caption = "${AppIcons.all.size} icons, drawn on a 24 grid at a uniform 1.8 stroke.",
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppIcons.all.forEach { (name, icon) ->
                Column(
                    modifier = Modifier
                        .size(width = 76.dp, height = 62.dp)
                        .padding(AppTheme.spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    AppIcon(icon, contentDescription = name, size = 24.dp)
                    AppText(
                        text = name,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.contentTertiary,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

private const val LOADING_DEMO_MILLIS = 2_000L
private const val OVERLAY_MILLIS = 1_400L
