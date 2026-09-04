package com.base.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/*
 * The raw palette. Rebranding a project starts and, in most cases, ends here.
 *
 * Nothing outside this file names a colour by its hue — screens ask for `AppTheme.colors.accent`
 * or `AppTheme.colors.danger.content`, never for "blue" or "red". That indirection is what lets
 * the whole app change accent in one edit, and it is also what makes a dark theme possible at
 * all: the same token resolves to a different value, and no call site changes.
 *
 * The neutrals carry a slight cool cast rather than being pure grey. On an OLED panel a pure
 * grey next to a saturated accent reads faintly brown; a few points of blue in the neutral keeps
 * the whole surface looking deliberate.
 */

// ── Neutrals · light ────────────────────────────────────────────────────────────
internal val White = Color(0xFFFFFFFF)
internal val Grey50 = Color(0xFFF7F8FA)
internal val Grey100 = Color(0xFFEFF1F5)
internal val Grey150 = Color(0xFFE9ECF1)
internal val Grey200 = Color(0xFFE3E6EC)
internal val Grey300 = Color(0xFFCBD1DB)
internal val Grey400 = Color(0xFFA3AAB6)
internal val Grey500 = Color(0xFF6B7280)
internal val Grey600 = Color(0xFF5A6472)
internal val Grey700 = Color(0xFF3D4552)
internal val Grey900 = Color(0xFF0B0F1A)

// ── Neutrals · dark ─────────────────────────────────────────────────────────────
internal val Ink900 = Color(0xFF0B0E14)
internal val Ink800 = Color(0xFF141922)
internal val Ink700 = Color(0xFF1D232E)
internal val Ink600 = Color(0xFF262D3A)
internal val Ink500 = Color(0xFF38414F)
internal val Ink400 = Color(0xFF565F6E)
internal val Ink300 = Color(0xFF7F8899)
internal val Ink200 = Color(0xFFB4BCCA)
internal val Ink100 = Color(0xFFF2F4F8)

// ── Accent ──────────────────────────────────────────────────────────────────────
internal val Accent = Color(0xFF2C6BED)
internal val AccentPressed = Color(0xFF1F52C4)
internal val AccentSubtleLight = Color(0xFFE8EFFD)
internal val AccentDark = Color(0xFF5B8DEF)
internal val AccentDarkPressed = Color(0xFF7BA5F5)
internal val AccentSubtleDark = Color(0xFF16233B)

// ── Status · light ──────────────────────────────────────────────────────────────
internal val SuccessLight = Color(0xFF12805C)
internal val SuccessSubtleLight = Color(0xFFE4F5EE)
internal val SuccessBorderLight = Color(0xFFB7E2D2)

internal val WarningLight = Color(0xFFA35B00)
internal val WarningSubtleLight = Color(0xFFFDF1E0)
internal val WarningBorderLight = Color(0xFFF2DDB8)

internal val DangerLight = Color(0xFFC42B32)
internal val DangerSubtleLight = Color(0xFFFCE9E9)
internal val DangerBorderLight = Color(0xFFF3C9CB)

internal val InfoLight = Color(0xFF1E5FBF)
internal val InfoSubtleLight = Color(0xFFE7F0FD)
internal val InfoBorderLight = Color(0xFFC3D8F7)

// ── Status · dark ───────────────────────────────────────────────────────────────
// Lifted in lightness and dropped in saturation. A colour tuned for white shown on near-black
// either vibrates or disappears; these are re-picked against Ink900 rather than reused.
internal val SuccessDark = Color(0xFF3DD4A0)
internal val SuccessSubtleDark = Color(0xFF10271F)
internal val SuccessBorderDark = Color(0xFF1E4638)

internal val WarningDark = Color(0xFFE9A23B)
internal val WarningSubtleDark = Color(0xFF2A1F0E)
internal val WarningBorderDark = Color(0xFF4A3616)

internal val DangerDark = Color(0xFFF0666D)
internal val DangerSubtleDark = Color(0xFF2B1416)
internal val DangerBorderDark = Color(0xFF4E2126)

internal val InfoDark = Color(0xFF6FA6FF)
internal val InfoSubtleDark = Color(0xFF10203A)
internal val InfoBorderDark = Color(0xFF1E3862)
