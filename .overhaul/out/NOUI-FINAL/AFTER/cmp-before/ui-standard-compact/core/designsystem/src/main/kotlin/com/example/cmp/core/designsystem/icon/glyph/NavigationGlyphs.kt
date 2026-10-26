package com.example.cmp.core.designsystem.icon.glyph

/** Arrows, chevrons and the glyphs of places in an app. Read through `AppIcons`. */
internal object NavigationGlyphs {
    val ArrowLeft by icon("ArrowLeft", autoMirror = true) {
        stroke { moveTo(20f, 12f); lineTo(4f, 12f); moveTo(10f, 6f); lineTo(4f, 12f); lineTo(10f, 18f) }
    }
    val ArrowRight by icon("ArrowRight", autoMirror = true) {
        stroke { moveTo(4f, 12f); lineTo(20f, 12f); moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f) }
    }
    val ArrowUp by icon("ArrowUp") {
        stroke { moveTo(12f, 20f); lineTo(12f, 4f); moveTo(6f, 10f); lineTo(12f, 4f); lineTo(18f, 10f) }
    }
    val ArrowDown by icon("ArrowDown") {
        stroke { moveTo(12f, 4f); lineTo(12f, 20f); moveTo(6f, 14f); lineTo(12f, 20f); lineTo(18f, 14f) }
    }
    val ChevronLeft by icon("ChevronLeft", autoMirror = true) {
        stroke { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    }
    val ChevronRight by icon("ChevronRight", autoMirror = true) {
        stroke { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    }
    val ChevronUp by icon("ChevronUp") {
        stroke { moveTo(5f, 15f); lineTo(12f, 8f); lineTo(19f, 15f) }
    }
    val ChevronDown by icon("ChevronDown") {
        stroke { moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f) }
    }

    val Home by icon("Home") {
        stroke { moveTo(3.5f, 10.4f); lineTo(12f, 3.4f); lineTo(20.5f, 10.4f); lineTo(20.5f, 20.4f); lineTo(3.5f, 20.4f); close() }
        stroke { moveTo(9.4f, 20.4f); lineTo(9.4f, 14f); lineTo(14.6f, 14f); lineTo(14.6f, 20.4f) }
    }
    val Grid by icon("Grid") {
        stroke {
            moveTo(4f, 4f); lineTo(10.4f, 4f); lineTo(10.4f, 10.4f); lineTo(4f, 10.4f); close()
            moveTo(13.6f, 4f); lineTo(20f, 4f); lineTo(20f, 10.4f); lineTo(13.6f, 10.4f); close()
            moveTo(4f, 13.6f); lineTo(10.4f, 13.6f); lineTo(10.4f, 20f); lineTo(4f, 20f); close()
            moveTo(13.6f, 13.6f); lineTo(20f, 13.6f); lineTo(20f, 20f); lineTo(13.6f, 20f); close()
        }
    }
    val ListView by icon("ListView") {
        stroke { moveTo(8.5f, 6.5f); lineTo(20f, 6.5f); moveTo(8.5f, 12f); lineTo(20f, 12f); moveTo(8.5f, 17.5f); lineTo(20f, 17.5f) }
        stroke(DotWidth) { dot(4.5f, 6.5f); dot(4.5f, 12f); dot(4.5f, 17.5f) }
    }
    val Search by icon("Search") {
        stroke { circle(10.5f, 10.5f, 6.6f) }
        stroke { moveTo(15.4f, 15.4f); lineTo(20.5f, 20.5f) }
    }
    val Filter by icon("Filter") {
        stroke { moveTo(3.5f, 6f); lineTo(20.5f, 6f); moveTo(6.5f, 12f); lineTo(17.5f, 12f); moveTo(10f, 18f); lineTo(14f, 18f) }
    }
    val Sliders by icon("Sliders") {
        stroke { moveTo(3.5f, 7.5f); lineTo(20.5f, 7.5f); moveTo(3.5f, 16.5f); lineTo(20.5f, 16.5f) }
        stroke { circle(9f, 7.5f, 2.4f); circle(15.5f, 16.5f, 2.4f) }
    }
    val Settings by icon("Settings") {
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
    val ExternalLink by icon("ExternalLink") {
        stroke { moveTo(13.5f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 10.5f) }
        stroke { moveTo(19.5f, 4.5f); lineTo(11f, 13f) }
        stroke { moveTo(18f, 14f); lineTo(18f, 19.5f); lineTo(4.5f, 19.5f); lineTo(4.5f, 6f); lineTo(10f, 6f) }
    }
}
