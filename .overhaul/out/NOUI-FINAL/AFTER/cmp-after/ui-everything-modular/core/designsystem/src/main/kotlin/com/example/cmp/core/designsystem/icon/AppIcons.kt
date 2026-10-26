package com.example.cmp.core.designsystem.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cmp.core.designsystem.icon.glyph.ActionsGlyphs
import com.example.cmp.core.designsystem.icon.glyph.ContentGlyphs
import com.example.cmp.core.designsystem.icon.glyph.FilledGlyphs
import com.example.cmp.core.designsystem.icon.glyph.NavigationGlyphs
import com.example.cmp.core.designsystem.icon.glyph.StatusGlyphs

/**
 * The icon set, drawn here rather than pulled from a library. Each glyph is built the first time
 * something draws it; the paths live in `glyph/`, one file per family.
 */
public object AppIcons {
    public val ArrowLeft: ImageVector get() = NavigationGlyphs.ArrowLeft
    public val ArrowRight: ImageVector get() = NavigationGlyphs.ArrowRight
    public val ArrowUp: ImageVector get() = NavigationGlyphs.ArrowUp
    public val ArrowDown: ImageVector get() = NavigationGlyphs.ArrowDown
    public val ChevronLeft: ImageVector get() = NavigationGlyphs.ChevronLeft
    public val ChevronRight: ImageVector get() = NavigationGlyphs.ChevronRight
    public val ChevronUp: ImageVector get() = NavigationGlyphs.ChevronUp
    public val ChevronDown: ImageVector get() = NavigationGlyphs.ChevronDown
    public val Close: ImageVector get() = ActionsGlyphs.Close
    public val Check: ImageVector get() = ActionsGlyphs.Check
    public val Plus: ImageVector get() = ActionsGlyphs.Plus
    public val Minus: ImageVector get() = ActionsGlyphs.Minus
    public val Menu: ImageVector get() = ActionsGlyphs.Menu
    public val MoreVertical: ImageVector get() = ActionsGlyphs.MoreVertical
    public val MoreHorizontal: ImageVector get() = ActionsGlyphs.MoreHorizontal
    public val Info: ImageVector get() = StatusGlyphs.Info
    public val AlertCircle: ImageVector get() = StatusGlyphs.AlertCircle
    public val AlertTriangle: ImageVector get() = StatusGlyphs.AlertTriangle
    public val CheckCircle: ImageVector get() = StatusGlyphs.CheckCircle
    public val XCircle: ImageVector get() = StatusGlyphs.XCircle
    public val HelpCircle: ImageVector get() = StatusGlyphs.HelpCircle
    public val Home: ImageVector get() = NavigationGlyphs.Home
    public val Grid: ImageVector get() = NavigationGlyphs.Grid
    public val ListView: ImageVector get() = NavigationGlyphs.ListView
    public val Search: ImageVector get() = NavigationGlyphs.Search
    public val Filter: ImageVector get() = NavigationGlyphs.Filter
    public val Sliders: ImageVector get() = NavigationGlyphs.Sliders
    public val Settings: ImageVector get() = NavigationGlyphs.Settings
    public val ExternalLink: ImageVector get() = NavigationGlyphs.ExternalLink
    public val User: ImageVector get() = ContentGlyphs.User
    public val Users: ImageVector get() = ContentGlyphs.Users
    public val Lock: ImageVector get() = ContentGlyphs.Lock
    public val Logout: ImageVector get() = ContentGlyphs.Logout
    public val Eye: ImageVector get() = ContentGlyphs.Eye
    public val EyeOff: ImageVector get() = ContentGlyphs.EyeOff
    public val Calendar: ImageVector get() = ContentGlyphs.Calendar
    public val Clock: ImageVector get() = ContentGlyphs.Clock
    public val Bell: ImageVector get() = ContentGlyphs.Bell
    public val Mail: ImageVector get() = ContentGlyphs.Mail
    public val Phone: ImageVector get() = ContentGlyphs.Phone
    public val Message: ImageVector get() = ContentGlyphs.Message
    public val Send: ImageVector get() = ContentGlyphs.Send
    public val Heart: ImageVector get() = ActionsGlyphs.Heart
    public val Star: ImageVector get() = ActionsGlyphs.Star
    public val StarFilled: ImageVector get() = ActionsGlyphs.StarFilled
    public val Bookmark: ImageVector get() = ActionsGlyphs.Bookmark
    public val Trash: ImageVector get() = ActionsGlyphs.Trash
    public val Edit: ImageVector get() = ActionsGlyphs.Edit
    public val Copy: ImageVector get() = ActionsGlyphs.Copy
    public val Share: ImageVector get() = ActionsGlyphs.Share
    public val Download: ImageVector get() = ActionsGlyphs.Download
    public val Upload: ImageVector get() = ActionsGlyphs.Upload
    public val Refresh: ImageVector get() = ActionsGlyphs.Refresh
    public val Camera: ImageVector get() = ContentGlyphs.Camera
    public val ImageIcon: ImageVector get() = ContentGlyphs.ImageIcon
    public val File: ImageVector get() = ContentGlyphs.File
    public val Folder: ImageVector get() = ContentGlyphs.Folder
    public val Cart: ImageVector get() = ContentGlyphs.Cart
    public val CreditCard: ImageVector get() = ContentGlyphs.CreditCard
    public val Tag: ImageVector get() = ContentGlyphs.Tag
    public val Package: ImageVector get() = ContentGlyphs.Package
    public val MapPin: ImageVector get() = ContentGlyphs.MapPin
    public val Globe: ImageVector get() = ContentGlyphs.Globe
    public val Wifi: ImageVector get() = ContentGlyphs.Wifi
    public val WifiOff: ImageVector get() = ContentGlyphs.WifiOff
    public val Sun: ImageVector get() = ContentGlyphs.Sun
    public val Moon: ImageVector get() = ContentGlyphs.Moon
    public val HomeFilled: ImageVector get() = FilledGlyphs.HomeFilled
    public val SearchFilled: ImageVector get() = FilledGlyphs.SearchFilled
    public val UserFilled: ImageVector get() = FilledGlyphs.UserFilled
    public val SettingsFilled: ImageVector get() = FilledGlyphs.SettingsFilled
    public val BellFilled: ImageVector get() = FilledGlyphs.BellFilled
    public val HeartFilled: ImageVector get() = FilledGlyphs.HeartFilled
    public val BookmarkFilled: ImageVector get() = FilledGlyphs.BookmarkFilled
    public val MessageFilled: ImageVector get() = FilledGlyphs.MessageFilled
    public val GridFilled: ImageVector get() = FilledGlyphs.GridFilled
    public val FolderFilled: ImageVector get() = FilledGlyphs.FolderFilled
    public val ListViewFilled: ImageVector get() = FilledGlyphs.ListViewFilled
    public val CalendarFilled: ImageVector get() = FilledGlyphs.CalendarFilled
    public val MailFilled: ImageVector get() = FilledGlyphs.MailFilled
    public val UsersFilled: ImageVector get() = FilledGlyphs.UsersFilled
    public val CartFilled: ImageVector get() = FilledGlyphs.CartFilled
    public val CreditCardFilled: ImageVector get() = FilledGlyphs.CreditCardFilled
    public val TagFilled: ImageVector get() = FilledGlyphs.TagFilled
    public val PackageFilled: ImageVector get() = FilledGlyphs.PackageFilled
    public val MapPinFilled: ImageVector get() = FilledGlyphs.MapPinFilled
    public val GlobeFilled: ImageVector get() = FilledGlyphs.GlobeFilled
}
