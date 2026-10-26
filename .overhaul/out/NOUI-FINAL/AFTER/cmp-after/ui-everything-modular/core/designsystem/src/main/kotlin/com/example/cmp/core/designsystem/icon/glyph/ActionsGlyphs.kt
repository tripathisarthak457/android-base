package com.example.cmp.core.designsystem.icon.glyph

/** Glyphs of things a person does: close, add, share, delete. Read through `AppIcons`. */
internal object ActionsGlyphs {
    val Close by icon("Close") {
        stroke {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }
    val Check by icon("Check") {
        stroke {
            moveTo(4.5f, 12.5f)
            lineTo(9.5f, 17.5f)
            lineTo(19.5f, 6.5f)
        }
    }
    val Plus by icon("Plus") {
        stroke {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }
    val Minus by icon("Minus") {
        stroke {
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }
    val Menu by icon("Menu") {
        stroke {
            moveTo(4f, 6.5f)
            lineTo(20f, 6.5f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 17.5f)
            lineTo(20f, 17.5f)
        }
    }
    val MoreVertical by icon("MoreVertical") {
        stroke(DotWidth) {
            dot(12f, 5.5f)
            dot(12f, 12f)
            dot(12f, 18.5f)
        }
    }
    val MoreHorizontal by icon("MoreHorizontal") {
        stroke(DotWidth) {
            dot(5.5f, 12f)
            dot(12f, 12f)
            dot(18.5f, 12f)
        }
    }

    val Heart by icon("Heart") {
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
    val Star by icon("Star") {
        stroke {
            moveTo(12f, 3.4f)
            lineTo(14.6f, 9.1f)
            lineTo(20.8f, 9.9f)
            lineTo(16.2f, 14.1f)
            lineTo(17.5f, 20.3f)
            lineTo(12f, 17.2f)
            lineTo(6.5f, 20.3f)
            lineTo(7.8f, 14.1f)
            lineTo(3.2f, 9.9f)
            lineTo(9.4f, 9.1f)
            close()
        }
    }
    val StarFilled by icon("StarFilled") {
        fill {
            moveTo(12f, 3.4f)
            lineTo(14.6f, 9.1f)
            lineTo(20.8f, 9.9f)
            lineTo(16.2f, 14.1f)
            lineTo(17.5f, 20.3f)
            lineTo(12f, 17.2f)
            lineTo(6.5f, 20.3f)
            lineTo(7.8f, 14.1f)
            lineTo(3.2f, 9.9f)
            lineTo(9.4f, 9.1f)
            close()
        }
    }
    val Bookmark by icon("Bookmark") {
        stroke {
            moveTo(6f, 3.6f)
            lineTo(18f, 3.6f)
            lineTo(18f, 20.4f)
            lineTo(12f, 15.4f)
            lineTo(6f, 20.4f)
            close()
        }
    }
    val Trash by icon("Trash") {
        stroke {
            moveTo(3.8f, 6.5f)
            lineTo(20.2f, 6.5f)
        }
        stroke {
            moveTo(9f, 6.5f)
            lineTo(9f, 3.8f)
            lineTo(15f, 3.8f)
            lineTo(15f, 6.5f)
        }
        stroke {
            moveTo(6.4f, 6.5f)
            lineTo(7.4f, 20.2f)
            lineTo(16.6f, 20.2f)
            lineTo(17.6f, 6.5f)
        }
        stroke {
            moveTo(10.4f, 10.4f)
            lineTo(10.4f, 16.6f)
            moveTo(13.6f, 10.4f)
            lineTo(13.6f, 16.6f)
        }
    }
    val Edit by icon("Edit") {
        stroke {
            moveTo(16.6f, 3.4f)
            lineTo(20.6f, 7.4f)
            lineTo(8f, 20f)
            lineTo(3.2f, 20.8f)
            lineTo(4f, 16f)
            close()
        }
        stroke {
            moveTo(14.4f, 5.6f)
            lineTo(18.4f, 9.6f)
        }
    }
    val Copy by icon("Copy") {
        stroke {
            moveTo(15f, 9f)
            lineTo(15f, 5f)
            curveTo(15f, 4.4f, 14.6f, 4f, 14f, 4f)
            lineTo(5f, 4f)
            curveTo(4.4f, 4f, 4f, 4.4f, 4f, 5f)
            lineTo(4f, 14f)
            curveTo(4f, 14.6f, 4.4f, 15f, 5f, 15f)
            lineTo(9f, 15f)
        }
        stroke {
            moveTo(10f, 9f)
            lineTo(19f, 9f)
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
    val Share by icon("Share") {
        stroke {
            circle(18f, 5.6f, 2.6f)
            circle(6f, 12f, 2.6f)
            circle(18f, 18.4f, 2.6f)
        }
        stroke {
            moveTo(8.3f, 10.7f)
            lineTo(15.7f, 6.9f)
            moveTo(8.3f, 13.3f)
            lineTo(15.7f, 17.1f)
        }
    }
    val Download by icon("Download") {
        stroke {
            moveTo(12f, 3.5f)
            lineTo(12f, 15.4f)
            moveTo(7f, 10.4f)
            lineTo(12f, 15.4f)
            lineTo(17f, 10.4f)
        }
        stroke {
            moveTo(4f, 20.4f)
            lineTo(20f, 20.4f)
        }
    }
    val Upload by icon("Upload") {
        stroke {
            moveTo(12f, 15.4f)
            lineTo(12f, 3.5f)
            moveTo(7f, 8.5f)
            lineTo(12f, 3.5f)
            lineTo(17f, 8.5f)
        }
        stroke {
            moveTo(4f, 20.4f)
            lineTo(20f, 20.4f)
        }
    }
    val Refresh by icon("Refresh") {
        stroke {
            moveTo(20.4f, 13.4f)
            curveTo(19.7f, 17.5f, 16.2f, 20.6f, 12f, 20.6f)
            curveTo(7.2f, 20.6f, 3.4f, 16.7f, 3.4f, 12f)
            curveTo(3.4f, 7.3f, 7.2f, 3.4f, 12f, 3.4f)
            curveTo(15.3f, 3.4f, 18.2f, 5.3f, 19.6f, 8f)
        }
        stroke {
            moveTo(20.6f, 3.4f)
            lineTo(19.9f, 8.4f)
            lineTo(15f, 7.6f)
        }
    }
}
