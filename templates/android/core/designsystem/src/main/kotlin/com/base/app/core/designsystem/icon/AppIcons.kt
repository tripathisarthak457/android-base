package com.base.app.core.designsystem.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.designsystem.icon.glyph.ActionsGlyphs
import com.base.app.core.designsystem.icon.glyph.ContentGlyphs
import com.base.app.core.designsystem.icon.glyph.FilledGlyphs
import com.base.app.core.designsystem.icon.glyph.NavigationGlyphs
import com.base.app.core.designsystem.icon.glyph.StatusGlyphs

/**
 * The icon set, drawn here rather than pulled from a library. Each glyph is built the first time
 * something draws it; the paths live in `glyph/`, one file per family.
 */
object AppIcons {
    val ArrowLeft: ImageVector get() = NavigationGlyphs.ArrowLeft
    val ArrowRight: ImageVector get() = NavigationGlyphs.ArrowRight
    val ArrowUp: ImageVector get() = NavigationGlyphs.ArrowUp
    val ArrowDown: ImageVector get() = NavigationGlyphs.ArrowDown
    val ChevronLeft: ImageVector get() = NavigationGlyphs.ChevronLeft
    val ChevronRight: ImageVector get() = NavigationGlyphs.ChevronRight
    val ChevronUp: ImageVector get() = NavigationGlyphs.ChevronUp
    val ChevronDown: ImageVector get() = NavigationGlyphs.ChevronDown
    val Close: ImageVector get() = ActionsGlyphs.Close
    val Check: ImageVector get() = ActionsGlyphs.Check
    val Plus: ImageVector get() = ActionsGlyphs.Plus
    val Minus: ImageVector get() = ActionsGlyphs.Minus
    val Menu: ImageVector get() = ActionsGlyphs.Menu
    val MoreVertical: ImageVector get() = ActionsGlyphs.MoreVertical
    val MoreHorizontal: ImageVector get() = ActionsGlyphs.MoreHorizontal
    val Info: ImageVector get() = StatusGlyphs.Info
    val AlertCircle: ImageVector get() = StatusGlyphs.AlertCircle
    val AlertTriangle: ImageVector get() = StatusGlyphs.AlertTriangle
    val CheckCircle: ImageVector get() = StatusGlyphs.CheckCircle
    val XCircle: ImageVector get() = StatusGlyphs.XCircle
    val HelpCircle: ImageVector get() = StatusGlyphs.HelpCircle
    val Home: ImageVector get() = NavigationGlyphs.Home
    val Grid: ImageVector get() = NavigationGlyphs.Grid
    val ListView: ImageVector get() = NavigationGlyphs.ListView
    val Search: ImageVector get() = NavigationGlyphs.Search
    val Filter: ImageVector get() = NavigationGlyphs.Filter
    val Sliders: ImageVector get() = NavigationGlyphs.Sliders
    val Settings: ImageVector get() = NavigationGlyphs.Settings
    val ExternalLink: ImageVector get() = NavigationGlyphs.ExternalLink
    val User: ImageVector get() = ContentGlyphs.User
    val Users: ImageVector get() = ContentGlyphs.Users
    val Lock: ImageVector get() = ContentGlyphs.Lock
    val Logout: ImageVector get() = ContentGlyphs.Logout
    val Eye: ImageVector get() = ContentGlyphs.Eye
    val EyeOff: ImageVector get() = ContentGlyphs.EyeOff
    val Calendar: ImageVector get() = ContentGlyphs.Calendar
    val Clock: ImageVector get() = ContentGlyphs.Clock
    val Bell: ImageVector get() = ContentGlyphs.Bell
    val Mail: ImageVector get() = ContentGlyphs.Mail
    val Phone: ImageVector get() = ContentGlyphs.Phone
    val Message: ImageVector get() = ContentGlyphs.Message
    val Send: ImageVector get() = ContentGlyphs.Send
    val Heart: ImageVector get() = ActionsGlyphs.Heart
    val Star: ImageVector get() = ActionsGlyphs.Star
    val StarFilled: ImageVector get() = ActionsGlyphs.StarFilled
    val Bookmark: ImageVector get() = ActionsGlyphs.Bookmark
    val Trash: ImageVector get() = ActionsGlyphs.Trash
    val Edit: ImageVector get() = ActionsGlyphs.Edit
    val Copy: ImageVector get() = ActionsGlyphs.Copy
    val Share: ImageVector get() = ActionsGlyphs.Share
    val Download: ImageVector get() = ActionsGlyphs.Download
    val Upload: ImageVector get() = ActionsGlyphs.Upload
    val Refresh: ImageVector get() = ActionsGlyphs.Refresh
    val Play: ImageVector get() = ActionsGlyphs.Play
    val Pause: ImageVector get() = ActionsGlyphs.Pause
    val VolumeOn: ImageVector get() = ActionsGlyphs.VolumeOn
    val VolumeOff: ImageVector get() = ActionsGlyphs.VolumeOff
    val Camera: ImageVector get() = ContentGlyphs.Camera
    val ImageIcon: ImageVector get() = ContentGlyphs.ImageIcon
    val File: ImageVector get() = ContentGlyphs.File
    val Folder: ImageVector get() = ContentGlyphs.Folder
    val Cart: ImageVector get() = ContentGlyphs.Cart
    val CreditCard: ImageVector get() = ContentGlyphs.CreditCard
    val Tag: ImageVector get() = ContentGlyphs.Tag
    val Package: ImageVector get() = ContentGlyphs.Package
    val MapPin: ImageVector get() = ContentGlyphs.MapPin
    val Globe: ImageVector get() = ContentGlyphs.Globe
    val Wifi: ImageVector get() = ContentGlyphs.Wifi
    val WifiOff: ImageVector get() = ContentGlyphs.WifiOff
    val Sun: ImageVector get() = ContentGlyphs.Sun
    val Moon: ImageVector get() = ContentGlyphs.Moon
    val HomeFilled: ImageVector get() = FilledGlyphs.HomeFilled
    val SearchFilled: ImageVector get() = FilledGlyphs.SearchFilled
    val UserFilled: ImageVector get() = FilledGlyphs.UserFilled
    val SettingsFilled: ImageVector get() = FilledGlyphs.SettingsFilled
    val BellFilled: ImageVector get() = FilledGlyphs.BellFilled
    val HeartFilled: ImageVector get() = FilledGlyphs.HeartFilled
    val BookmarkFilled: ImageVector get() = FilledGlyphs.BookmarkFilled
    val MessageFilled: ImageVector get() = FilledGlyphs.MessageFilled
    val GridFilled: ImageVector get() = FilledGlyphs.GridFilled
    val FolderFilled: ImageVector get() = FilledGlyphs.FolderFilled
    val ListViewFilled: ImageVector get() = FilledGlyphs.ListViewFilled
    val CalendarFilled: ImageVector get() = FilledGlyphs.CalendarFilled
    val MailFilled: ImageVector get() = FilledGlyphs.MailFilled
    val UsersFilled: ImageVector get() = FilledGlyphs.UsersFilled
    val CartFilled: ImageVector get() = FilledGlyphs.CartFilled
    val CreditCardFilled: ImageVector get() = FilledGlyphs.CreditCardFilled
    val TagFilled: ImageVector get() = FilledGlyphs.TagFilled
    val PackageFilled: ImageVector get() = FilledGlyphs.PackageFilled
    val MapPinFilled: ImageVector get() = FilledGlyphs.MapPinFilled
    val GlobeFilled: ImageVector get() = FilledGlyphs.GlobeFilled
}
