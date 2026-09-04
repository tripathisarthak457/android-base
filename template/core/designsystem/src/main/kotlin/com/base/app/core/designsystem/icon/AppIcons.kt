package com.base.app.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

/**
 * The icon set, drawn here rather than pulled from a library.
 *
 * `material-icons-extended` is roughly 30 000 vectors and ~10 MB of dex before shrinking, it is
 * unmistakably one visual language, and depending on it would reintroduce the Material artifact
 * this design system exists without. These are authored on a 24 grid with a uniform 1.8 stroke
 * and round caps, so they sit together at any size and inherit the surrounding content colour
 * through `AppIcon`'s tint.
 *
 * Adding one is a `val` and a path. Keep to the same grid and stroke width — an icon at a
 * different weight is instantly visible in a row next to the others, even when nobody can say
 * why.
 */
object AppIcons {

    // ── Direction ───────────────────────────────────────────────────────────────
    val ArrowLeft = icon("ArrowLeft") {
        stroke { moveTo(20f, 12f); lineTo(4f, 12f); moveTo(10f, 6f); lineTo(4f, 12f); lineTo(10f, 18f) }
    }
    val ArrowRight = icon("ArrowRight") {
        stroke { moveTo(4f, 12f); lineTo(20f, 12f); moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f) }
    }
    val ArrowUp = icon("ArrowUp") {
        stroke { moveTo(12f, 20f); lineTo(12f, 4f); moveTo(6f, 10f); lineTo(12f, 4f); lineTo(18f, 10f) }
    }
    val ArrowDown = icon("ArrowDown") {
        stroke { moveTo(12f, 4f); lineTo(12f, 20f); moveTo(6f, 14f); lineTo(12f, 20f); lineTo(18f, 14f) }
    }
    val ChevronLeft = icon("ChevronLeft") {
        stroke { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    }
    val ChevronRight = icon("ChevronRight") {
        stroke { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    }
    val ChevronUp = icon("ChevronUp") {
        stroke { moveTo(5f, 15f); lineTo(12f, 8f); lineTo(19f, 15f) }
    }
    val ChevronDown = icon("ChevronDown") {
        stroke { moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f) }
    }

    // ── Primitives ──────────────────────────────────────────────────────────────
    val Close = icon("Close") {
        stroke { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    }
    val Check = icon("Check") {
        stroke { moveTo(4.5f, 12.5f); lineTo(9.5f, 17.5f); lineTo(19.5f, 6.5f) }
    }
    val Plus = icon("Plus") {
        stroke { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    }
    val Minus = icon("Minus") {
        stroke { moveTo(5f, 12f); lineTo(19f, 12f) }
    }
    val Menu = icon("Menu") {
        stroke { moveTo(4f, 6.5f); lineTo(20f, 6.5f); moveTo(4f, 12f); lineTo(20f, 12f); moveTo(4f, 17.5f); lineTo(20f, 17.5f) }
    }
    val MoreVertical = icon("MoreVertical") {
        stroke(DOT_WIDTH) { dot(12f, 5.5f); dot(12f, 12f); dot(12f, 18.5f) }
    }
    val MoreHorizontal = icon("MoreHorizontal") {
        stroke(DOT_WIDTH) { dot(5.5f, 12f); dot(12f, 12f); dot(18.5f, 12f) }
    }

    // ── Status ──────────────────────────────────────────────────────────────────
    val Info = icon("Info") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(12f, 11f); lineTo(12f, 16.5f) }
        stroke(DOT_WIDTH) { dot(12f, 7.9f) }
    }
    val AlertCircle = icon("AlertCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(12f, 7.3f); lineTo(12f, 13f) }
        stroke(DOT_WIDTH) { dot(12f, 16.4f) }
    }
    val AlertTriangle = icon("AlertTriangle") {
        stroke { moveTo(12f, 3.6f); lineTo(21.8f, 20.4f); lineTo(2.2f, 20.4f); close() }
        stroke { moveTo(12f, 9.6f); lineTo(12f, 14.6f) }
        stroke(DOT_WIDTH) { dot(12f, 17.6f) }
    }
    val CheckCircle = icon("CheckCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(7.8f, 12.3f); lineTo(10.8f, 15.3f); lineTo(16.2f, 9.4f) }
    }
    val XCircle = icon("XCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(9f, 9f); lineTo(15f, 15f); moveTo(15f, 9f); lineTo(9f, 15f) }
    }
    val HelpCircle = icon("HelpCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke {
            moveTo(9.4f, 9.6f)
            curveTo(9.6f, 8.2f, 10.7f, 7.2f, 12f, 7.2f)
            curveTo(13.5f, 7.2f, 14.7f, 8.4f, 14.7f, 9.9f)
            curveTo(14.7f, 11.7f, 12f, 12.2f, 12f, 14.2f)
        }
        stroke(DOT_WIDTH) { dot(12f, 17f) }
    }

    // ── Navigation ──────────────────────────────────────────────────────────────
    val Home = icon("Home") {
        stroke { moveTo(3.5f, 10.4f); lineTo(12f, 3.4f); lineTo(20.5f, 10.4f); lineTo(20.5f, 20.4f); lineTo(3.5f, 20.4f); close() }
        stroke { moveTo(9.4f, 20.4f); lineTo(9.4f, 14f); lineTo(14.6f, 14f); lineTo(14.6f, 20.4f) }
    }
    val Grid = icon("Grid") {
        stroke {
            moveTo(4f, 4f); lineTo(10.4f, 4f); lineTo(10.4f, 10.4f); lineTo(4f, 10.4f); close()
            moveTo(13.6f, 4f); lineTo(20f, 4f); lineTo(20f, 10.4f); lineTo(13.6f, 10.4f); close()
            moveTo(4f, 13.6f); lineTo(10.4f, 13.6f); lineTo(10.4f, 20f); lineTo(4f, 20f); close()
            moveTo(13.6f, 13.6f); lineTo(20f, 13.6f); lineTo(20f, 20f); lineTo(13.6f, 20f); close()
        }
    }
    val ListView = icon("ListView") {
        stroke { moveTo(8.5f, 6.5f); lineTo(20f, 6.5f); moveTo(8.5f, 12f); lineTo(20f, 12f); moveTo(8.5f, 17.5f); lineTo(20f, 17.5f) }
        stroke(DOT_WIDTH) { dot(4.5f, 6.5f); dot(4.5f, 12f); dot(4.5f, 17.5f) }
    }
    val Search = icon("Search") {
        stroke { circle(10.5f, 10.5f, 6.6f) }
        stroke { moveTo(15.4f, 15.4f); lineTo(20.5f, 20.5f) }
    }
    val Filter = icon("Filter") {
        stroke { moveTo(3.5f, 6f); lineTo(20.5f, 6f); moveTo(6.5f, 12f); lineTo(17.5f, 12f); moveTo(10f, 18f); lineTo(14f, 18f) }
    }
    val Sliders = icon("Sliders") {
        stroke { moveTo(3.5f, 7.5f); lineTo(20.5f, 7.5f); moveTo(3.5f, 16.5f); lineTo(20.5f, 16.5f) }
        stroke { circle(9f, 7.5f, 2.4f); circle(15.5f, 16.5f, 2.4f) }
    }
    val Settings = icon("Settings") {
        stroke { circle(12f, 12f, 3.2f) }
        stroke {
            moveTo(19.1f, 14.6f)
            curveTo(18.9f, 15.1f, 19f, 15.6f, 19.4f, 16f)
            lineTo(19.5f, 16.1f)
            curveTo(20.1f, 16.7f, 20.1f, 17.6f, 19.5f, 18.2f)
            curveTo(18.9f, 18.8f, 18f, 18.8f, 17.4f, 18.2f)
            lineTo(17.3f, 18.1f)
            curveTo(16.9f, 17.7f, 16.4f, 17.6f, 15.9f, 17.8f)
            curveTo(15.4f, 18f, 15.1f, 18.5f, 15.1f, 19f)
            lineTo(15.1f, 19.2f)
            curveTo(15.1f, 20.1f, 14.4f, 20.8f, 13.5f, 20.8f)
            lineTo(10.5f, 20.8f)
            curveTo(9.6f, 20.8f, 8.9f, 20.1f, 8.9f, 19.2f)
            lineTo(8.9f, 19f)
            curveTo(8.9f, 18.5f, 8.6f, 18f, 8.1f, 17.8f)
            curveTo(7.6f, 17.6f, 7.1f, 17.7f, 6.7f, 18.1f)
            lineTo(6.6f, 18.2f)
            curveTo(6f, 18.8f, 5.1f, 18.8f, 4.5f, 18.2f)
            curveTo(3.9f, 17.6f, 3.9f, 16.7f, 4.5f, 16.1f)
            lineTo(4.6f, 16f)
            curveTo(5f, 15.6f, 5.1f, 15.1f, 4.9f, 14.6f)
            curveTo(4.7f, 14.1f, 4.2f, 13.8f, 3.7f, 13.8f)
            lineTo(3.6f, 13.8f)
            curveTo(2.7f, 13.8f, 2f, 13.1f, 2f, 12.2f)
            lineTo(2f, 11.8f)
            curveTo(2f, 10.9f, 2.7f, 10.2f, 3.6f, 10.2f)
            lineTo(3.7f, 10.2f)
            curveTo(4.2f, 10.2f, 4.7f, 9.9f, 4.9f, 9.4f)
            curveTo(5.1f, 8.9f, 5f, 8.4f, 4.6f, 8f)
            lineTo(4.5f, 7.9f)
            curveTo(3.9f, 7.3f, 3.9f, 6.4f, 4.5f, 5.8f)
            curveTo(5.1f, 5.2f, 6f, 5.2f, 6.6f, 5.8f)
            lineTo(6.7f, 5.9f)
            curveTo(7.1f, 6.3f, 7.6f, 6.4f, 8.1f, 6.2f)
            curveTo(8.6f, 6f, 8.9f, 5.5f, 8.9f, 5f)
            lineTo(8.9f, 4.8f)
            curveTo(8.9f, 3.9f, 9.6f, 3.2f, 10.5f, 3.2f)
            lineTo(13.5f, 3.2f)
            curveTo(14.4f, 3.2f, 15.1f, 3.9f, 15.1f, 4.8f)
            lineTo(15.1f, 5f)
            curveTo(15.1f, 5.5f, 15.4f, 6f, 15.9f, 6.2f)
            curveTo(16.4f, 6.4f, 16.9f, 6.3f, 17.3f, 5.9f)
            lineTo(17.4f, 5.8f)
            curveTo(18f, 5.2f, 18.9f, 5.2f, 19.5f, 5.8f)
            curveTo(20.1f, 6.4f, 20.1f, 7.3f, 19.5f, 7.9f)
            lineTo(19.4f, 8f)
            curveTo(19f, 8.4f, 18.9f, 8.9f, 19.1f, 9.4f)
            curveTo(19.3f, 9.9f, 19.8f, 10.2f, 20.3f, 10.2f)
            lineTo(20.4f, 10.2f)
            curveTo(21.3f, 10.2f, 22f, 10.9f, 22f, 11.8f)
            lineTo(22f, 12.2f)
            curveTo(22f, 13.1f, 21.3f, 13.8f, 20.4f, 13.8f)
            lineTo(20.3f, 13.8f)
            curveTo(19.8f, 13.8f, 19.3f, 14.1f, 19.1f, 14.6f)
            close()
        }
    }
    val ExternalLink = icon("ExternalLink") {
        stroke { moveTo(13.5f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 10.5f) }
        stroke { moveTo(19.5f, 4.5f); lineTo(11f, 13f) }
        stroke { moveTo(18f, 14f); lineTo(18f, 19.5f); lineTo(4.5f, 19.5f); lineTo(4.5f, 6f); lineTo(10f, 6f) }
    }

    // ── People and account ──────────────────────────────────────────────────────
    val User = icon("User") {
        stroke { circle(12f, 8f, 4f) }
        stroke {
            moveTo(4.5f, 20.5f)
            curveTo(4.5f, 16.4f, 7.9f, 14f, 12f, 14f)
            curveTo(16.1f, 14f, 19.5f, 16.4f, 19.5f, 20.5f)
        }
    }
    val Users = icon("Users") {
        stroke { circle(9.5f, 8f, 3.6f) }
        stroke {
            moveTo(2.8f, 20.5f)
            curveTo(2.8f, 16.7f, 5.8f, 14.4f, 9.5f, 14.4f)
            curveTo(13.2f, 14.4f, 16.2f, 16.7f, 16.2f, 20.5f)
        }
        stroke {
            moveTo(16f, 5.2f)
            curveTo(17.8f, 5.7f, 19f, 7.3f, 19f, 9.2f)
            curveTo(19f, 10.4f, 18.5f, 11.5f, 17.7f, 12.3f)
        }
        stroke { moveTo(18.2f, 15.4f); curveTo(20.1f, 16.3f, 21.2f, 18.1f, 21.2f, 20.5f) }
    }
    val Lock = icon("Lock") {
        stroke { moveTo(4.8f, 10.5f); lineTo(19.2f, 10.5f); lineTo(19.2f, 20.5f); lineTo(4.8f, 20.5f); close() }
        stroke {
            moveTo(8f, 10.5f); lineTo(8f, 7.6f)
            curveTo(8f, 5.4f, 9.8f, 3.5f, 12f, 3.5f)
            curveTo(14.2f, 3.5f, 16f, 5.4f, 16f, 7.6f)
            lineTo(16f, 10.5f)
        }
    }
    val Logout = icon("Logout") {
        stroke { moveTo(10f, 4.5f); lineTo(4.5f, 4.5f); lineTo(4.5f, 19.5f); lineTo(10f, 19.5f) }
        stroke { moveTo(9.5f, 12f); lineTo(20.5f, 12f); moveTo(16.5f, 8f); lineTo(20.5f, 12f); lineTo(16.5f, 16f) }
    }
    val Eye = icon("Eye") {
        stroke {
            moveTo(2.2f, 12f)
            curveTo(4.6f, 7.5f, 8.1f, 5.2f, 12f, 5.2f)
            curveTo(15.9f, 5.2f, 19.4f, 7.5f, 21.8f, 12f)
            curveTo(19.4f, 16.5f, 15.9f, 18.8f, 12f, 18.8f)
            curveTo(8.1f, 18.8f, 4.6f, 16.5f, 2.2f, 12f)
            close()
        }
        stroke { circle(12f, 12f, 3.2f) }
    }
    val EyeOff = icon("EyeOff") {
        stroke {
            moveTo(9.6f, 5.6f)
            curveTo(10.4f, 5.3f, 11.2f, 5.2f, 12f, 5.2f)
            curveTo(15.9f, 5.2f, 19.4f, 7.5f, 21.8f, 12f)
            curveTo(20.8f, 13.9f, 19.6f, 15.4f, 18.2f, 16.5f)
        }
        stroke {
            moveTo(15.2f, 18.4f)
            curveTo(14.2f, 18.7f, 13.1f, 18.8f, 12f, 18.8f)
            curveTo(8.1f, 18.8f, 4.6f, 16.5f, 2.2f, 12f)
            curveTo(3.4f, 9.8f, 4.8f, 8.1f, 6.5f, 7f)
        }
        stroke { moveTo(3.5f, 3.5f); lineTo(20.5f, 20.5f) }
    }

    // ── Time ────────────────────────────────────────────────────────────────────
    val Calendar = icon("Calendar") {
        stroke { moveTo(3.6f, 5.6f); lineTo(20.4f, 5.6f); lineTo(20.4f, 20.4f); lineTo(3.6f, 20.4f); close() }
        stroke { moveTo(3.6f, 9.8f); lineTo(20.4f, 9.8f) }
        stroke { moveTo(8f, 3.2f); lineTo(8f, 7f); moveTo(16f, 3.2f); lineTo(16f, 7f) }
    }
    val Clock = icon("Clock") {
        stroke { circle(12f, 12f, 8.8f) }
        stroke { moveTo(12f, 6.8f); lineTo(12f, 12.3f); lineTo(15.8f, 14.5f) }
    }

    // ── Communication ───────────────────────────────────────────────────────────
    val Bell = icon("Bell") {
        stroke {
            moveTo(6.4f, 10.2f)
            curveTo(6.4f, 7.1f, 8.9f, 4.6f, 12f, 4.6f)
            curveTo(15.1f, 4.6f, 17.6f, 7.1f, 17.6f, 10.2f)
            lineTo(17.6f, 15f)
            lineTo(19.6f, 17.6f)
            lineTo(4.4f, 17.6f)
            lineTo(6.4f, 15f)
            close()
        }
        stroke { moveTo(10f, 20.2f); curveTo(10.5f, 21.2f, 13.5f, 21.2f, 14f, 20.2f) }
    }
    val Mail = icon("Mail") {
        stroke { moveTo(3.2f, 5.6f); lineTo(20.8f, 5.6f); lineTo(20.8f, 18.4f); lineTo(3.2f, 18.4f); close() }
        stroke { moveTo(3.6f, 6.6f); lineTo(12f, 12.8f); lineTo(20.4f, 6.6f) }
    }
    val Phone = icon("Phone") {
        stroke {
            moveTo(20.8f, 17f)
            lineTo(20.8f, 19.6f)
            curveTo(20.8f, 20.4f, 20.1f, 21f, 19.3f, 20.9f)
            curveTo(10.7f, 20.3f, 3.7f, 13.3f, 3.1f, 4.7f)
            curveTo(3f, 3.9f, 3.6f, 3.2f, 4.4f, 3.2f)
            lineTo(7f, 3.2f)
            curveTo(7.7f, 3.2f, 8.3f, 3.7f, 8.4f, 4.4f)
            curveTo(8.6f, 5.7f, 8.9f, 6.9f, 9.4f, 8f)
            curveTo(9.6f, 8.5f, 9.5f, 9f, 9.1f, 9.4f)
            lineTo(7.7f, 10.8f)
            curveTo(9.2f, 13.6f, 10.4f, 14.8f, 13.2f, 16.3f)
            lineTo(14.6f, 14.9f)
            curveTo(15f, 14.5f, 15.5f, 14.4f, 16f, 14.6f)
            curveTo(17.1f, 15.1f, 18.3f, 15.4f, 19.6f, 15.6f)
            curveTo(20.3f, 15.7f, 20.8f, 16.3f, 20.8f, 17f)
            close()
        }
    }
    val Message = icon("Message") {
        stroke {
            moveTo(20.6f, 11.6f)
            curveTo(20.6f, 16.3f, 16.7f, 20.1f, 12f, 20.1f)
            curveTo(10.6f, 20.1f, 9.3f, 19.8f, 8.1f, 19.2f)
            lineTo(3.4f, 20.6f)
            lineTo(4.9f, 16.2f)
            curveTo(4f, 14.9f, 3.4f, 13.3f, 3.4f, 11.6f)
            curveTo(3.4f, 6.9f, 7.3f, 3.1f, 12f, 3.1f)
            curveTo(16.7f, 3.1f, 20.6f, 6.9f, 20.6f, 11.6f)
            close()
        }
    }
    val Send = icon("Send") {
        stroke { moveTo(21f, 3f); lineTo(10.5f, 13.5f) }
        stroke { moveTo(21f, 3f); lineTo(14.3f, 21f); lineTo(10.5f, 13.5f); lineTo(3f, 9.7f); close() }
    }

    // ── Content ─────────────────────────────────────────────────────────────────
    val Heart = icon("Heart") {
        stroke {
            moveTo(12f, 20.4f)
            curveTo(12f, 20.4f, 3.2f, 15f, 3.2f, 9f)
            curveTo(3.2f, 6.3f, 5.4f, 4.1f, 8.1f, 4.1f)
            curveTo(9.7f, 4.1f, 11.2f, 4.9f, 12f, 6.2f)
            curveTo(12.8f, 4.9f, 14.3f, 4.1f, 15.9f, 4.1f)
            curveTo(18.6f, 4.1f, 20.8f, 6.3f, 20.8f, 9f)
            curveTo(20.8f, 15f, 12f, 20.4f, 12f, 20.4f)
            close()
        }
    }
    val Star = icon("Star") {
        stroke {
            moveTo(12f, 3.4f); lineTo(14.6f, 9.1f); lineTo(20.8f, 9.9f); lineTo(16.2f, 14.1f)
            lineTo(17.5f, 20.3f); lineTo(12f, 17.2f); lineTo(6.5f, 20.3f); lineTo(7.8f, 14.1f)
            lineTo(3.2f, 9.9f); lineTo(9.4f, 9.1f); close()
        }
    }
    val StarFilled = icon("StarFilled") {
        fill {
            moveTo(12f, 3.4f); lineTo(14.6f, 9.1f); lineTo(20.8f, 9.9f); lineTo(16.2f, 14.1f)
            lineTo(17.5f, 20.3f); lineTo(12f, 17.2f); lineTo(6.5f, 20.3f); lineTo(7.8f, 14.1f)
            lineTo(3.2f, 9.9f); lineTo(9.4f, 9.1f); close()
        }
    }
    val Bookmark = icon("Bookmark") {
        stroke { moveTo(6f, 3.6f); lineTo(18f, 3.6f); lineTo(18f, 20.4f); lineTo(12f, 15.4f); lineTo(6f, 20.4f); close() }
    }
    val Trash = icon("Trash") {
        stroke { moveTo(3.8f, 6.5f); lineTo(20.2f, 6.5f) }
        stroke { moveTo(9f, 6.5f); lineTo(9f, 3.8f); lineTo(15f, 3.8f); lineTo(15f, 6.5f) }
        stroke { moveTo(6.4f, 6.5f); lineTo(7.4f, 20.2f); lineTo(16.6f, 20.2f); lineTo(17.6f, 6.5f) }
        stroke { moveTo(10.4f, 10.4f); lineTo(10.4f, 16.6f); moveTo(13.6f, 10.4f); lineTo(13.6f, 16.6f) }
    }
    val Edit = icon("Edit") {
        stroke { moveTo(16.6f, 3.4f); lineTo(20.6f, 7.4f); lineTo(8f, 20f); lineTo(3.2f, 20.8f); lineTo(4f, 16f); close() }
        stroke { moveTo(14.4f, 5.6f); lineTo(18.4f, 9.6f) }
    }
    val Copy = icon("Copy") {
        stroke {
            moveTo(15f, 9f); lineTo(15f, 5f)
            curveTo(15f, 4.4f, 14.6f, 4f, 14f, 4f)
            lineTo(5f, 4f)
            curveTo(4.4f, 4f, 4f, 4.4f, 4f, 5f)
            lineTo(4f, 14f)
            curveTo(4f, 14.6f, 4.4f, 15f, 5f, 15f)
            lineTo(9f, 15f)
        }
        stroke {
            moveTo(10f, 9f); lineTo(19f, 9f)
            curveTo(19.6f, 9f, 20f, 9.4f, 20f, 10f)
            lineTo(20f, 19f)
            curveTo(20f, 19.6f, 19.6f, 20f, 19f, 20f)
            lineTo(10f, 20f)
            curveTo(9.4f, 20f, 9f, 19.6f, 9f, 19f)
            lineTo(9f, 10f)
            curveTo(9f, 9.4f, 9.4f, 9f, 10f, 9f)
            close()
        }
    }
    val Share = icon("Share") {
        stroke { circle(18f, 5.6f, 2.6f); circle(6f, 12f, 2.6f); circle(18f, 18.4f, 2.6f) }
        stroke { moveTo(8.3f, 10.7f); lineTo(15.7f, 6.9f); moveTo(8.3f, 13.3f); lineTo(15.7f, 17.1f) }
    }
    val Download = icon("Download") {
        stroke { moveTo(12f, 3.5f); lineTo(12f, 15.4f); moveTo(7f, 10.4f); lineTo(12f, 15.4f); lineTo(17f, 10.4f) }
        stroke { moveTo(4f, 20.4f); lineTo(20f, 20.4f) }
    }
    val Upload = icon("Upload") {
        stroke { moveTo(12f, 15.4f); lineTo(12f, 3.5f); moveTo(7f, 8.5f); lineTo(12f, 3.5f); lineTo(17f, 8.5f) }
        stroke { moveTo(4f, 20.4f); lineTo(20f, 20.4f) }
    }
    val Refresh = icon("Refresh") {
        stroke {
            moveTo(20.4f, 13.4f)
            curveTo(19.7f, 17.5f, 16.2f, 20.6f, 12f, 20.6f)
            curveTo(7.2f, 20.6f, 3.4f, 16.7f, 3.4f, 12f)
            curveTo(3.4f, 7.3f, 7.2f, 3.4f, 12f, 3.4f)
            curveTo(15.3f, 3.4f, 18.2f, 5.3f, 19.6f, 8f)
        }
        stroke { moveTo(20.6f, 3.4f); lineTo(19.9f, 8.4f); lineTo(15f, 7.6f) }
    }

    // ── Media and files ─────────────────────────────────────────────────────────
    val Camera = icon("Camera") {
        stroke {
            moveTo(3.2f, 8.4f); lineTo(7.6f, 8.4f); lineTo(9.3f, 5.4f); lineTo(14.7f, 5.4f)
            lineTo(16.4f, 8.4f); lineTo(20.8f, 8.4f); lineTo(20.8f, 19.2f); lineTo(3.2f, 19.2f); close()
        }
        stroke { circle(12f, 13.4f, 3.6f) }
    }
    val ImageIcon = icon("ImageIcon") {
        stroke { moveTo(3.6f, 4.6f); lineTo(20.4f, 4.6f); lineTo(20.4f, 19.4f); lineTo(3.6f, 19.4f); close() }
        stroke { circle(8.8f, 9.4f, 1.9f) }
        stroke { moveTo(20.4f, 16f); lineTo(15.4f, 11f); lineTo(5.2f, 19.4f) }
    }
    val File = icon("File") {
        stroke { moveTo(6f, 3.6f); lineTo(14f, 3.6f); lineTo(19f, 8.6f); lineTo(19f, 20.4f); lineTo(6f, 20.4f); close() }
        stroke { moveTo(14f, 3.6f); lineTo(14f, 8.6f); lineTo(19f, 8.6f) }
    }
    val Folder = icon("Folder") {
        stroke {
            moveTo(3.2f, 6.6f); lineTo(9.4f, 6.6f); lineTo(11.4f, 9.2f); lineTo(20.8f, 9.2f)
            lineTo(20.8f, 19.4f); lineTo(3.2f, 19.4f); close()
        }
    }

    // ── Commerce ────────────────────────────────────────────────────────────────
    val Cart = icon("Cart") {
        stroke { moveTo(2.6f, 4f); lineTo(5.4f, 4f); lineTo(7.8f, 15.2f); lineTo(19f, 15.2f); lineTo(21.4f, 7.2f); lineTo(6.2f, 7.2f) }
        stroke { circle(9f, 19.2f, 1.6f); circle(17.6f, 19.2f, 1.6f) }
    }
    val CreditCard = icon("CreditCard") {
        stroke { moveTo(2.8f, 5.4f); lineTo(21.2f, 5.4f); lineTo(21.2f, 18.6f); lineTo(2.8f, 18.6f); close() }
        stroke { moveTo(2.8f, 9.8f); lineTo(21.2f, 9.8f) }
        stroke { moveTo(6.4f, 14.6f); lineTo(10f, 14.6f) }
    }
    val Tag = icon("Tag") {
        stroke {
            moveTo(11.2f, 3.2f); lineTo(20.8f, 12.8f); lineTo(12.8f, 20.8f); lineTo(3.2f, 11.2f)
            lineTo(3.2f, 3.2f); close()
        }
        stroke(DOT_WIDTH) { dot(7.4f, 7.4f) }
    }
    val Package = icon("Package") {
        stroke { moveTo(12f, 3.2f); lineTo(20.8f, 7.6f); lineTo(20.8f, 16.4f); lineTo(12f, 20.8f); lineTo(3.2f, 16.4f); lineTo(3.2f, 7.6f); close() }
        stroke { moveTo(3.2f, 7.6f); lineTo(12f, 12f); lineTo(20.8f, 7.6f); moveTo(12f, 12f); lineTo(12f, 20.8f) }
    }

    // ── Location and connectivity ───────────────────────────────────────────────
    val MapPin = icon("MapPin") {
        stroke {
            moveTo(12f, 21f)
            curveTo(12f, 21f, 19f, 15.2f, 19f, 10.2f)
            curveTo(19f, 6.3f, 15.9f, 3.2f, 12f, 3.2f)
            curveTo(8.1f, 3.2f, 5f, 6.3f, 5f, 10.2f)
            curveTo(5f, 15.2f, 12f, 21f, 12f, 21f)
            close()
        }
        stroke { circle(12f, 10f, 2.6f) }
    }
    val Globe = icon("Globe") {
        stroke { circle(12f, 12f, 8.8f) }
        stroke { moveTo(3.2f, 12f); lineTo(20.8f, 12f) }
        stroke {
            moveTo(12f, 3.2f)
            curveTo(14.4f, 5.6f, 15.6f, 8.8f, 15.6f, 12f)
            curveTo(15.6f, 15.2f, 14.4f, 18.4f, 12f, 20.8f)
            curveTo(9.6f, 18.4f, 8.4f, 15.2f, 8.4f, 12f)
            curveTo(8.4f, 8.8f, 9.6f, 5.6f, 12f, 3.2f)
            close()
        }
    }
    val Wifi = icon("Wifi") {
        stroke { moveTo(2.4f, 8.6f); curveTo(8f, 3.8f, 16f, 3.8f, 21.6f, 8.6f) }
        stroke { moveTo(5.8f, 12.4f); curveTo(9.5f, 9.3f, 14.5f, 9.3f, 18.2f, 12.4f) }
        stroke { moveTo(9.2f, 16.2f); curveTo(10.9f, 14.8f, 13.1f, 14.8f, 14.8f, 16.2f) }
        stroke(DOT_WIDTH) { dot(12f, 19.6f) }
    }
    val WifiOff = icon("WifiOff") {
        stroke { moveTo(3.5f, 3.5f); lineTo(20.5f, 20.5f) }
        stroke { moveTo(9.2f, 16.2f); curveTo(10.9f, 14.8f, 13.1f, 14.8f, 14.8f, 16.2f) }
        stroke { moveTo(2.4f, 8.6f); curveTo(4f, 7.2f, 5.8f, 6.2f, 7.7f, 5.6f) }
        stroke { moveTo(13.4f, 5.1f); curveTo(16.5f, 5.4f, 19.4f, 6.6f, 21.6f, 8.6f) }
        stroke(DOT_WIDTH) { dot(12f, 19.6f) }
    }

    // ── Theme ───────────────────────────────────────────────────────────────────
    val Sun = icon("Sun") {
        stroke { circle(12f, 12f, 4.4f) }
        stroke {
            moveTo(12f, 1.8f); lineTo(12f, 4f); moveTo(12f, 20f); lineTo(12f, 22.2f)
            moveTo(1.8f, 12f); lineTo(4f, 12f); moveTo(20f, 12f); lineTo(22.2f, 12f)
            moveTo(4.8f, 4.8f); lineTo(6.4f, 6.4f); moveTo(17.6f, 17.6f); lineTo(19.2f, 19.2f)
            moveTo(19.2f, 4.8f); lineTo(17.6f, 6.4f); moveTo(6.4f, 17.6f); lineTo(4.8f, 19.2f)
        }
    }
    val Moon = icon("Moon") {
        stroke {
            moveTo(20.6f, 14.4f)
            curveTo(19.3f, 15f, 17.9f, 15.3f, 16.4f, 15.3f)
            curveTo(10.9f, 15.3f, 6.5f, 10.9f, 6.5f, 5.4f)
            curveTo(6.5f, 4.7f, 6.6f, 4f, 6.7f, 3.4f)
            curveTo(4.2f, 5f, 2.6f, 7.8f, 2.6f, 11f)
            curveTo(2.6f, 16.2f, 6.8f, 20.4f, 12f, 20.4f)
            curveTo(15.9f, 20.4f, 19.2f, 18f, 20.6f, 14.4f)
            close()
        }
    }

    /** Everything above, for the catalog's icon grid. Order is the order they are declared. */
    val all: List<Pair<String, ImageVector>> = listOf(
        "ArrowLeft" to ArrowLeft, "ArrowRight" to ArrowRight, "ArrowUp" to ArrowUp,
        "ArrowDown" to ArrowDown, "ChevronLeft" to ChevronLeft, "ChevronRight" to ChevronRight,
        "ChevronUp" to ChevronUp, "ChevronDown" to ChevronDown, "Close" to Close,
        "Check" to Check, "Plus" to Plus, "Minus" to Minus, "Menu" to Menu,
        "MoreVertical" to MoreVertical, "MoreHorizontal" to MoreHorizontal, "Info" to Info,
        "AlertCircle" to AlertCircle, "AlertTriangle" to AlertTriangle,
        "CheckCircle" to CheckCircle, "XCircle" to XCircle, "HelpCircle" to HelpCircle,
        "Home" to Home, "Grid" to Grid, "ListView" to ListView, "Search" to Search,
        "Filter" to Filter, "Sliders" to Sliders, "Settings" to Settings,
        "ExternalLink" to ExternalLink, "User" to User, "Users" to Users, "Lock" to Lock,
        "Logout" to Logout, "Eye" to Eye, "EyeOff" to EyeOff, "Calendar" to Calendar,
        "Clock" to Clock, "Bell" to Bell, "Mail" to Mail, "Phone" to Phone,
        "Message" to Message, "Send" to Send, "Heart" to Heart, "Star" to Star,
        "StarFilled" to StarFilled, "Bookmark" to Bookmark, "Trash" to Trash, "Edit" to Edit,
        "Copy" to Copy, "Share" to Share, "Download" to Download, "Upload" to Upload,
        "Refresh" to Refresh, "Camera" to Camera, "ImageIcon" to ImageIcon, "File" to File,
        "Folder" to Folder, "Cart" to Cart, "CreditCard" to CreditCard, "Tag" to Tag,
        "Package" to Package, "MapPin" to MapPin, "Globe" to Globe, "Wifi" to Wifi,
        "WifiOff" to WifiOff, "Sun" to Sun, "Moon" to Moon,
    )
}

private const val STROKE_WIDTH = 1.8f

/** Dots are drawn as a zero-length round-capped stroke, so the width *is* the diameter. */
private const val DOT_WIDTH = 2.4f

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun ImageVector.Builder.stroke(
    width: Float = STROKE_WIDTH,
    path: PathBuilder.() -> Unit,
): ImageVector.Builder = addPath(
    pathData = PathData(path),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
)

private fun ImageVector.Builder.fill(path: PathBuilder.() -> Unit): ImageVector.Builder =
    addPath(pathData = PathData(path), fill = SolidColor(Color.Black))

/** Two half-arcs, because the vector format has no circle primitive. */
private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcTo(radius, radius, 0f, true, true, centerX + radius, centerY)
    arcTo(radius, radius, 0f, true, true, centerX - radius, centerY)
    close()
}

private fun PathBuilder.dot(x: Float, y: Float) {
    moveTo(x, y)
    lineTo(x + 0.01f, y)
}
