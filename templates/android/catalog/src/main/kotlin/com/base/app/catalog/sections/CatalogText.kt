package com.base.app.catalog.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.base.app.catalog.CatalogGroup
import com.base.app.catalog.fontScaled
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppExpandableText
import com.base.app.core.designsystem.component.text.AppFiguresText
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

@Composable
fun TextSection() {
    CatalogGroup("AppText", caption = "Everything inherits body. autoSize shrinks a line to fit its box.") {
        AppText(text = "Body text, the default style", style = AppTheme.typography.body)
        AppText(
            text = "A headline that shrinks to fit",
            modifier = Modifier.width(180.dp),
            style = AppTheme.typography.title2,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 22.sp),
        )
    }
    CatalogGroup("Colour read while drawing", caption = "The ColorProducer overloads of AppText and AppIcon: a colour change redraws and recomposes nothing.") {
        var on by remember { mutableStateOf(false) }
        val tint = animateColorAsState(if (on) AppTheme.colors.accentContent else AppTheme.colors.contentSecondary, label = "catalogTint")
        AppText(text = "Selected", color = { tint.value }, style = AppTheme.typography.headline)
        AppIcon(AppIcons.Star, contentDescription = null, tint = { tint.value })
        AppButton(text = "Toggle", onClick = { on = !on }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
    }
    CatalogGroup("Figures", caption = "AppFiguresText keeps every digit one width; AppMonoText is for codes read character by character.") {
        AppFiguresText(text = "12 items · 3 left · 1,240.50", style = AppTheme.typography.body)
        AppFiguresText(text = "1,111.11", style = AppTheme.typography.title3)
        AppFiguresText(text = "9,999.99", style = AppTheme.typography.title3)
        AppMonoText(text = "ORD-2026-0042")
    }
    CatalogGroup("AppExpandableText") {
        AppExpandableText(
            text = "A description long enough to be cut at three lines. The Show more link appears only when something " +
                "was cut, measured rather than guessed from the length, and the height follows on the layout spring. " +
                "It is for a detail screen's description, not for rows in a list, where a row that resizes moves every row below it.",
        )
    }
    CatalogGroup("Icons", caption = "Built the first time they are drawn. Directional ones mirror in right-to-left layouts; tab glyphs have filled forms.") {
        val cell = fontScaled(72.dp)
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AllIcons.forEach { (name, icon) ->
                Column(Modifier.width(cell), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppIcon(icon, contentDescription = null, size = AppTheme.sizes.iconLarge)
                    AppText(
                        text = name,
                        style = AppTheme.typography.caption2,
                        color = AppTheme.colors.contentSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val AllIcons = listOf(
    "ArrowLeft" to AppIcons.ArrowLeft,
    "ArrowRight" to AppIcons.ArrowRight,
    "ArrowUp" to AppIcons.ArrowUp,
    "ArrowDown" to AppIcons.ArrowDown,
    "ChevronLeft" to AppIcons.ChevronLeft,
    "ChevronRight" to AppIcons.ChevronRight,
    "ChevronUp" to AppIcons.ChevronUp,
    "ChevronDown" to AppIcons.ChevronDown,
    "Close" to AppIcons.Close,
    "Check" to AppIcons.Check,
    "Plus" to AppIcons.Plus,
    "Minus" to AppIcons.Minus,
    "Menu" to AppIcons.Menu,
    "MoreVertical" to AppIcons.MoreVertical,
    "MoreHorizontal" to AppIcons.MoreHorizontal,
    "Info" to AppIcons.Info,
    "AlertCircle" to AppIcons.AlertCircle,
    "AlertTriangle" to AppIcons.AlertTriangle,
    "CheckCircle" to AppIcons.CheckCircle,
    "XCircle" to AppIcons.XCircle,
    "HelpCircle" to AppIcons.HelpCircle,
    "Home" to AppIcons.Home,
    "Grid" to AppIcons.Grid,
    "ListView" to AppIcons.ListView,
    "Search" to AppIcons.Search,
    "Filter" to AppIcons.Filter,
    "Sliders" to AppIcons.Sliders,
    "Settings" to AppIcons.Settings,
    "ExternalLink" to AppIcons.ExternalLink,
    "User" to AppIcons.User,
    "Users" to AppIcons.Users,
    "Lock" to AppIcons.Lock,
    "Logout" to AppIcons.Logout,
    "Eye" to AppIcons.Eye,
    "EyeOff" to AppIcons.EyeOff,
    "Calendar" to AppIcons.Calendar,
    "Clock" to AppIcons.Clock,
    "Bell" to AppIcons.Bell,
    "Mail" to AppIcons.Mail,
    "Phone" to AppIcons.Phone,
    "Message" to AppIcons.Message,
    "Send" to AppIcons.Send,
    "Heart" to AppIcons.Heart,
    "Star" to AppIcons.Star,
    "StarFilled" to AppIcons.StarFilled,
    "Bookmark" to AppIcons.Bookmark,
    "Trash" to AppIcons.Trash,
    "Edit" to AppIcons.Edit,
    "Copy" to AppIcons.Copy,
    "Share" to AppIcons.Share,
    "Download" to AppIcons.Download,
    "Upload" to AppIcons.Upload,
    "Refresh" to AppIcons.Refresh,
    "Camera" to AppIcons.Camera,
    "ImageIcon" to AppIcons.ImageIcon,
    "File" to AppIcons.File,
    "Folder" to AppIcons.Folder,
    "Cart" to AppIcons.Cart,
    "CreditCard" to AppIcons.CreditCard,
    "Tag" to AppIcons.Tag,
    "Package" to AppIcons.Package,
    "MapPin" to AppIcons.MapPin,
    "Globe" to AppIcons.Globe,
    "Wifi" to AppIcons.Wifi,
    "WifiOff" to AppIcons.WifiOff,
    "Sun" to AppIcons.Sun,
    "Moon" to AppIcons.Moon,
    "HomeFilled" to AppIcons.HomeFilled,
    "SearchFilled" to AppIcons.SearchFilled,
    "UserFilled" to AppIcons.UserFilled,
    "SettingsFilled" to AppIcons.SettingsFilled,
    "BellFilled" to AppIcons.BellFilled,
    "HeartFilled" to AppIcons.HeartFilled,
    "BookmarkFilled" to AppIcons.BookmarkFilled,
    "MessageFilled" to AppIcons.MessageFilled,
    "GridFilled" to AppIcons.GridFilled,
    "FolderFilled" to AppIcons.FolderFilled,
    "ListViewFilled" to AppIcons.ListViewFilled,
    "CalendarFilled" to AppIcons.CalendarFilled,
    "MailFilled" to AppIcons.MailFilled,
    "UsersFilled" to AppIcons.UsersFilled,
    "CartFilled" to AppIcons.CartFilled,
    "CreditCardFilled" to AppIcons.CreditCardFilled,
    "TagFilled" to AppIcons.TagFilled,
    "PackageFilled" to AppIcons.PackageFilled,
    "MapPinFilled" to AppIcons.MapPinFilled,
    "GlobeFilled" to AppIcons.GlobeFilled,
)
