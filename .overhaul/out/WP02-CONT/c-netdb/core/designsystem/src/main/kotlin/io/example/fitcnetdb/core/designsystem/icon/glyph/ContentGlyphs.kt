package io.example.fitcnetdb.core.designsystem.icon.glyph

/** Glyphs of things: people, time, messages, media, commerce, places, the theme. Read through `AppIcons`. */
internal object ContentGlyphs {
    val User by icon("User") {
        stroke { circle(12f, 8f, 4f) }
        stroke {
            moveTo(4.5f, 20.5f)
            curveTo(4.5f, 16.4f, 7.9f, 14f, 12f, 14f)
            curveTo(16.1f, 14f, 19.5f, 16.4f, 19.5f, 20.5f)
        }
    }
    val Users by icon("Users") {
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
    val Lock by icon("Lock") {
        stroke { moveTo(4.8f, 10.5f); lineTo(19.2f, 10.5f); lineTo(19.2f, 20.5f); lineTo(4.8f, 20.5f); close() }
        stroke {
            moveTo(8f, 10.5f); lineTo(8f, 7.6f)
            curveTo(8f, 5.4f, 9.8f, 3.5f, 12f, 3.5f)
            curveTo(14.2f, 3.5f, 16f, 5.4f, 16f, 7.6f)
            lineTo(16f, 10.5f)
        }
    }
    val Logout by icon("Logout") {
        stroke { moveTo(10f, 4.5f); lineTo(4.5f, 4.5f); lineTo(4.5f, 19.5f); lineTo(10f, 19.5f) }
        stroke { moveTo(9.5f, 12f); lineTo(20.5f, 12f); moveTo(16.5f, 8f); lineTo(20.5f, 12f); lineTo(16.5f, 16f) }
    }
    val Eye by icon("Eye") {
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
    val EyeOff by icon("EyeOff") {
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

    val Calendar by icon("Calendar") {
        stroke { moveTo(3.6f, 5.6f); lineTo(20.4f, 5.6f); lineTo(20.4f, 20.4f); lineTo(3.6f, 20.4f); close() }
        stroke { moveTo(3.6f, 9.8f); lineTo(20.4f, 9.8f) }
        stroke { moveTo(8f, 3.2f); lineTo(8f, 7f); moveTo(16f, 3.2f); lineTo(16f, 7f) }
    }
    val Clock by icon("Clock") {
        stroke { circle(12f, 12f, 8.8f) }
        stroke { moveTo(12f, 6.8f); lineTo(12f, 12.3f); lineTo(15.8f, 14.5f) }
    }

    val Bell by icon("Bell") {
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
    val Mail by icon("Mail") {
        stroke { moveTo(3.2f, 5.6f); lineTo(20.8f, 5.6f); lineTo(20.8f, 18.4f); lineTo(3.2f, 18.4f); close() }
        stroke { moveTo(3.6f, 6.6f); lineTo(12f, 12.8f); lineTo(20.4f, 6.6f) }
    }
    val Phone by icon("Phone") {
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
    val Message by icon("Message") {
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
    val Send by icon("Send", autoMirror = true) {
        stroke { moveTo(21f, 3f); lineTo(10.5f, 13.5f) }
        stroke { moveTo(21f, 3f); lineTo(14.3f, 21f); lineTo(10.5f, 13.5f); lineTo(3f, 9.7f); close() }
    }

    val Camera by icon("Camera") {
        stroke {
            moveTo(3.2f, 8.4f); lineTo(7.6f, 8.4f); lineTo(9.3f, 5.4f); lineTo(14.7f, 5.4f)
            lineTo(16.4f, 8.4f); lineTo(20.8f, 8.4f); lineTo(20.8f, 19.2f); lineTo(3.2f, 19.2f); close()
        }
        stroke { circle(12f, 13.4f, 3.6f) }
    }
    val ImageIcon by icon("ImageIcon") {
        stroke { moveTo(3.6f, 4.6f); lineTo(20.4f, 4.6f); lineTo(20.4f, 19.4f); lineTo(3.6f, 19.4f); close() }
        stroke { circle(8.8f, 9.4f, 1.9f) }
        stroke { moveTo(20.4f, 16f); lineTo(15.4f, 11f); lineTo(5.2f, 19.4f) }
    }
    val File by icon("File") {
        stroke { moveTo(6f, 3.6f); lineTo(14f, 3.6f); lineTo(19f, 8.6f); lineTo(19f, 20.4f); lineTo(6f, 20.4f); close() }
        stroke { moveTo(14f, 3.6f); lineTo(14f, 8.6f); lineTo(19f, 8.6f) }
    }
    val Folder by icon("Folder") {
        stroke {
            moveTo(3.2f, 6.6f); lineTo(9.4f, 6.6f); lineTo(11.4f, 9.2f); lineTo(20.8f, 9.2f)
            lineTo(20.8f, 19.4f); lineTo(3.2f, 19.4f); close()
        }
    }

    val Cart by icon("Cart") {
        stroke { moveTo(2.6f, 4f); lineTo(5.4f, 4f); lineTo(7.8f, 15.2f); lineTo(19f, 15.2f); lineTo(21.4f, 7.2f); lineTo(6.2f, 7.2f) }
        stroke { circle(9f, 19.2f, 1.6f); circle(17.6f, 19.2f, 1.6f) }
    }
    val CreditCard by icon("CreditCard") {
        stroke { moveTo(2.8f, 5.4f); lineTo(21.2f, 5.4f); lineTo(21.2f, 18.6f); lineTo(2.8f, 18.6f); close() }
        stroke { moveTo(2.8f, 9.8f); lineTo(21.2f, 9.8f) }
        stroke { moveTo(6.4f, 14.6f); lineTo(10f, 14.6f) }
    }
    val Tag by icon("Tag") {
        stroke {
            moveTo(11.2f, 3.2f); lineTo(20.8f, 12.8f); lineTo(12.8f, 20.8f); lineTo(3.2f, 11.2f)
            lineTo(3.2f, 3.2f); close()
        }
        stroke(DotWidth) { dot(7.4f, 7.4f) }
    }
    val Package by icon("Package") {
        stroke { moveTo(12f, 3.2f); lineTo(20.8f, 7.6f); lineTo(20.8f, 16.4f); lineTo(12f, 20.8f); lineTo(3.2f, 16.4f); lineTo(3.2f, 7.6f); close() }
        stroke { moveTo(3.2f, 7.6f); lineTo(12f, 12f); lineTo(20.8f, 7.6f); moveTo(12f, 12f); lineTo(12f, 20.8f) }
    }

    val MapPin by icon("MapPin") {
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
    val Globe by icon("Globe") {
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
    val Wifi by icon("Wifi") {
        stroke { moveTo(2.4f, 8.6f); curveTo(8f, 3.8f, 16f, 3.8f, 21.6f, 8.6f) }
        stroke { moveTo(5.8f, 12.4f); curveTo(9.5f, 9.3f, 14.5f, 9.3f, 18.2f, 12.4f) }
        stroke { moveTo(9.2f, 16.2f); curveTo(10.9f, 14.8f, 13.1f, 14.8f, 14.8f, 16.2f) }
        stroke(DotWidth) { dot(12f, 19.6f) }
    }
    val WifiOff by icon("WifiOff") {
        stroke { moveTo(3.5f, 3.5f); lineTo(20.5f, 20.5f) }
        stroke { moveTo(9.2f, 16.2f); curveTo(10.9f, 14.8f, 13.1f, 14.8f, 14.8f, 16.2f) }
        stroke { moveTo(2.4f, 8.6f); curveTo(4f, 7.2f, 5.8f, 6.2f, 7.7f, 5.6f) }
        stroke { moveTo(13.4f, 5.1f); curveTo(16.5f, 5.4f, 19.4f, 6.6f, 21.6f, 8.6f) }
        stroke(DotWidth) { dot(12f, 19.6f) }
    }

    val Sun by icon("Sun") {
        stroke { circle(12f, 12f, 4.4f) }
        stroke {
            moveTo(12f, 1.8f); lineTo(12f, 4f); moveTo(12f, 20f); lineTo(12f, 22.2f)
            moveTo(1.8f, 12f); lineTo(4f, 12f); moveTo(20f, 12f); lineTo(22.2f, 12f)
            moveTo(4.8f, 4.8f); lineTo(6.4f, 6.4f); moveTo(17.6f, 17.6f); lineTo(19.2f, 19.2f)
            moveTo(19.2f, 4.8f); lineTo(17.6f, 6.4f); moveTo(6.4f, 17.6f); lineTo(4.8f, 19.2f)
        }
    }
    val Moon by icon("Moon") {
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
}
