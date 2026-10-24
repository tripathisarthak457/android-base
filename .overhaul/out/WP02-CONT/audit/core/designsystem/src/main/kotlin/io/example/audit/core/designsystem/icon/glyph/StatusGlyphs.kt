package io.example.audit.core.designsystem.icon.glyph

/** Glyphs that state an outcome: information, a warning, success, failure. Read through `AppIcons`. */
internal object StatusGlyphs {
    val Info by icon("Info") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(12f, 11f); lineTo(12f, 16.5f) }
        stroke(DotWidth) { dot(12f, 7.9f) }
    }
    val AlertCircle by icon("AlertCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(12f, 7.3f); lineTo(12f, 13f) }
        stroke(DotWidth) { dot(12f, 16.4f) }
    }
    val AlertTriangle by icon("AlertTriangle") {
        stroke { moveTo(12f, 3.6f); lineTo(21.8f, 20.4f); lineTo(2.2f, 20.4f); close() }
        stroke { moveTo(12f, 9.6f); lineTo(12f, 14.6f) }
        stroke(DotWidth) { dot(12f, 17.6f) }
    }
    val CheckCircle by icon("CheckCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(7.8f, 12.3f); lineTo(10.8f, 15.3f); lineTo(16.2f, 9.4f) }
    }
    val XCircle by icon("XCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke { moveTo(9f, 9f); lineTo(15f, 15f); moveTo(15f, 9f); lineTo(9f, 15f) }
    }
    val HelpCircle by icon("HelpCircle") {
        stroke { circle(12f, 12f, 9f) }
        stroke {
            moveTo(9.4f, 9.6f)
            curveTo(9.6f, 8.2f, 10.7f, 7.2f, 12f, 7.2f)
            curveTo(13.5f, 7.2f, 14.7f, 8.4f, 14.7f, 9.9f)
            curveTo(14.7f, 11.7f, 12f, 12.2f, 12f, 14.2f)
        }
        stroke(DotWidth) { dot(12f, 17f) }
    }
}
