package com.example.videoalone.core.designsystem.icon.glyph

/** Filled forms, for a selected tab. Read through `AppIcons`. */
internal object FilledGlyphs {
    val HomeFilled by icon("HomeFilled") {
        fill(evenOdd = true) {
            moveTo(3.5f, 10.4f); lineTo(12f, 3.4f); lineTo(20.5f, 10.4f); lineTo(20.5f, 20.4f); lineTo(3.5f, 20.4f); close()
            moveTo(9.4f, 20.4f); lineTo(9.4f, 14f); lineTo(14.6f, 14f); lineTo(14.6f, 20.4f); close()
        }
        stroke { moveTo(3.5f, 10.4f); lineTo(12f, 3.4f); lineTo(20.5f, 10.4f); lineTo(20.5f, 20.4f); lineTo(3.5f, 20.4f); close() }
    }
    val SearchFilled by icon("SearchFilled") {
        stroke(BoldStrokeWidth) { circle(10.5f, 10.5f, 6.4f) }
        stroke(BoldStrokeWidth) { moveTo(15.4f, 15.4f); lineTo(20.3f, 20.3f) }
    }
    val UserFilled by icon("UserFilled") {
        fill { circle(12f, 8f, 4.4f) }
        fill {
            moveTo(3.8f, 20.8f)
            curveTo(3.8f, 16.2f, 7.5f, 13.5f, 12f, 13.5f)
            curveTo(16.5f, 13.5f, 20.2f, 16.2f, 20.2f, 20.8f)
            close()
        }
    }
    val SettingsFilled by icon("SettingsFilled") {
        fill(evenOdd = true) {
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
            circle(12f, 12f, 3.2f)
        }
    }
    val BellFilled by icon("BellFilled") {
        fill {
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
    val HeartFilled by icon("HeartFilled") {
        fill {
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
    val BookmarkFilled by icon("BookmarkFilled") {
        fill { moveTo(6f, 3.6f); lineTo(18f, 3.6f); lineTo(18f, 20.4f); lineTo(12f, 15.4f); lineTo(6f, 20.4f); close() }
    }
    val MessageFilled by icon("MessageFilled") {
        fill {
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
    val GridFilled by icon("GridFilled") {
        fill {
            moveTo(4f, 4f); lineTo(10.4f, 4f); lineTo(10.4f, 10.4f); lineTo(4f, 10.4f); close()
            moveTo(13.6f, 4f); lineTo(20f, 4f); lineTo(20f, 10.4f); lineTo(13.6f, 10.4f); close()
            moveTo(4f, 13.6f); lineTo(10.4f, 13.6f); lineTo(10.4f, 20f); lineTo(4f, 20f); close()
            moveTo(13.6f, 13.6f); lineTo(20f, 13.6f); lineTo(20f, 20f); lineTo(13.6f, 20f); close()
        }
    }
    val FolderFilled by icon("FolderFilled") {
        fill {
            moveTo(3.2f, 6.6f); lineTo(9.4f, 6.6f); lineTo(11.4f, 9.2f); lineTo(20.8f, 9.2f)
            lineTo(20.8f, 19.4f); lineTo(3.2f, 19.4f); close()
        }
    }
    val ListViewFilled by icon("ListViewFilled") {
        stroke(BoldStrokeWidth) { moveTo(9f, 6.5f); lineTo(20f, 6.5f); moveTo(9f, 12f); lineTo(20f, 12f); moveTo(9f, 17.5f); lineTo(20f, 17.5f) }
        fill { circle(4.6f, 6.5f, 1.7f); circle(4.6f, 12f, 1.7f); circle(4.6f, 17.5f, 1.7f) }
    }
    val CalendarFilled by icon("CalendarFilled") {
        // The header and the days are one card, parted by a cut where the outline draws a rule.
        fill(evenOdd = true) {
            moveTo(3.6f, 5.6f); lineTo(20.4f, 5.6f); lineTo(20.4f, 20.4f); lineTo(3.6f, 20.4f); close()
            moveTo(3.6f, 9.3f); lineTo(20.4f, 9.3f); lineTo(20.4f, 10.7f); lineTo(3.6f, 10.7f); close()
        }
        stroke { moveTo(3.6f, 5.6f); lineTo(20.4f, 5.6f); lineTo(20.4f, 20.4f); lineTo(3.6f, 20.4f); close() }
        stroke { moveTo(8f, 3.2f); lineTo(8f, 6f); moveTo(16f, 3.2f); lineTo(16f, 6f) }
    }
    val MailFilled by icon("MailFilled") {
        fill(evenOdd = true) {
            moveTo(3.2f, 5.6f); lineTo(20.8f, 5.6f); lineTo(20.8f, 18.4f); lineTo(3.2f, 18.4f); close()
            moveTo(3.2f, 6.4f); lineTo(12f, 12.7f); lineTo(20.8f, 6.4f); lineTo(20.8f, 8.2f); lineTo(12f, 14.5f); lineTo(3.2f, 8.2f); close()
        }
        stroke { moveTo(3.2f, 5.6f); lineTo(20.8f, 5.6f); lineTo(20.8f, 18.4f); lineTo(3.2f, 18.4f); close() }
    }
    val UsersFilled by icon("UsersFilled") {
        fill { circle(9.2f, 8.2f, 3.8f) }
        fill {
            moveTo(2.6f, 20.6f)
            curveTo(2.6f, 16.6f, 5.6f, 14.2f, 9.2f, 14.2f)
            curveTo(12.8f, 14.2f, 15.8f, 16.6f, 15.8f, 20.6f)
            close()
        }
        fill { circle(16.6f, 8.8f, 2.9f) }
        fill {
            moveTo(16.8f, 14.4f)
            curveTo(19.5f, 14.8f, 21.4f, 16.9f, 21.4f, 20.6f)
            lineTo(17.4f, 20.6f)
            curveTo(17.4f, 18.2f, 16.6f, 16.3f, 15.2f, 15f)
            close()
        }
    }
    val CartFilled by icon("CartFilled") {
        fill { moveTo(6.2f, 7.2f); lineTo(21.4f, 7.2f); lineTo(19f, 15.2f); lineTo(7.8f, 15.2f); close() }
        stroke { moveTo(2.6f, 4f); lineTo(5.4f, 4f); lineTo(7.8f, 15.2f); lineTo(19f, 15.2f); lineTo(21.4f, 7.2f); lineTo(6.2f, 7.2f) }
        fill { circle(9f, 19.2f, 1.9f); circle(17.6f, 19.2f, 1.9f) }
    }
    val CreditCardFilled by icon("CreditCardFilled") {
        fill(evenOdd = true) {
            moveTo(2.8f, 5.4f); lineTo(21.2f, 5.4f); lineTo(21.2f, 18.6f); lineTo(2.8f, 18.6f); close()
            moveTo(2.8f, 8.6f); lineTo(21.2f, 8.6f); lineTo(21.2f, 10.8f); lineTo(2.8f, 10.8f); close()
            moveTo(6.2f, 13.8f); lineTo(10.4f, 13.8f); lineTo(10.4f, 15.4f); lineTo(6.2f, 15.4f); close()
        }
        stroke { moveTo(2.8f, 5.4f); lineTo(21.2f, 5.4f); lineTo(21.2f, 18.6f); lineTo(2.8f, 18.6f); close() }
    }
    val TagFilled by icon("TagFilled") {
        fill(evenOdd = true) {
            moveTo(11.2f, 3.2f); lineTo(20.8f, 12.8f); lineTo(12.8f, 20.8f); lineTo(3.2f, 11.2f); lineTo(3.2f, 3.2f); close()
            circle(7.4f, 7.4f, 1.5f)
        }
        stroke { moveTo(11.2f, 3.2f); lineTo(20.8f, 12.8f); lineTo(12.8f, 20.8f); lineTo(3.2f, 11.2f); lineTo(3.2f, 3.2f); close() }
    }
    val PackageFilled by icon("PackageFilled") {
        // Three faces with a gap between them, which is how a filled box shows its edges.
        fill { moveTo(12f, 3.2f); lineTo(20.8f, 7.6f); lineTo(12f, 12f); lineTo(3.2f, 7.6f); close() }
        fill { moveTo(3.2f, 8.8f); lineTo(11.4f, 12.9f); lineTo(11.4f, 20.6f); lineTo(3.2f, 16.4f); close() }
        fill { moveTo(20.8f, 8.8f); lineTo(12.6f, 12.9f); lineTo(12.6f, 20.6f); lineTo(20.8f, 16.4f); close() }
    }
    val MapPinFilled by icon("MapPinFilled") {
        fill(evenOdd = true) {
            moveTo(12f, 21.4f)
            curveTo(12f, 21.4f, 19.6f, 15.4f, 19.6f, 10.2f)
            curveTo(19.6f, 6f, 16.2f, 2.6f, 12f, 2.6f)
            curveTo(7.8f, 2.6f, 4.4f, 6f, 4.4f, 10.2f)
            curveTo(4.4f, 15.4f, 12f, 21.4f, 12f, 21.4f)
            close()
            circle(12f, 10f, 2.6f)
        }
    }
    val GlobeFilled by icon("GlobeFilled") {
        stroke(BoldStrokeWidth) { circle(12f, 12f, 8.8f) }
        stroke(BoldStrokeWidth) { moveTo(3.2f, 12f); lineTo(20.8f, 12f) }
        stroke(BoldStrokeWidth) {
            moveTo(12f, 3.2f)
            curveTo(14.4f, 5.6f, 15.6f, 8.8f, 15.6f, 12f)
            curveTo(15.6f, 15.2f, 14.4f, 18.4f, 12f, 20.8f)
            curveTo(9.6f, 18.4f, 8.4f, 15.2f, 8.4f, 12f)
            curveTo(8.4f, 8.8f, 9.6f, 5.6f, 12f, 3.2f)
            close()
        }
    }
}
