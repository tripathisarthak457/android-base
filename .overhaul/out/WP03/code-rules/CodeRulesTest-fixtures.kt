        // Sheet rules (SheetRules.kt) and scroll rules (ScrollRules.kt): add these to CodeRulesTest's `fixtures`.
        Fixture(
            "sheet-ime-padding",
            FEATURE,
            """
            @Composable
            private fun Rename() = AppSheet(visible = true, onDismissRequest = {}) {
                SheetColumn(Modifier.imePadding()) { } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture("sheet-shadow", SHEET, "private fun Modifier.card() = this.shadow(8.dp) /*!*/"),
        Fixture("sheet-raw-delta", SHEET, "fun drag(s: AnchoredDraggableState<Int>) = s.dispatchRawDelta(1f) /*!*/"),
        Fixture("sheet-window", SHEET, "@Composable fun Card() { Popup(onDismissRequest = {}) { } } /*!*/"),
        Fixture(
            "sheet-conditional",
            FEATURE,
            """
            @Composable
            private fun Orders(show: Boolean) {
                if (show) {
                    AppSheet(visible = true, onDismissRequest = {}) { } /*!*/
                }
            }
            """.trimIndent(),
        ),
        Fixture(
            "sheet-modifier",
            FEATURE,
            """
            @Composable
            private fun Orders() {
                AppSheet(visible = true, onDismissRequest = {}, modifier = Modifier.fillMaxWidth()) { } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture(
            "sheet-root",
            FEATURE,
            """
            @Composable
            private fun Orders() {
                AppSheet(visible = true, onDismissRequest = {}) {
                    Header()
                    Body() /*!*/
                }
            }
            """.trimIndent(),
        ),
        Fixture("shell-seam", FEATURE, "@OptIn(ShellSeamApi::class) /*!*/"),
        Fixture(
            "sheet-when-else",
            FEATURE,
            """
            private fun label(d: SheetDetent) = when (d) { /*!*/
                SheetDetent.Large -> "large"
                SheetDetent.Medium -> "medium"
            }
            """.trimIndent(),
        ),

        Fixture("scroll-fling", CORE_UI, "private fun Photos() = LazyRow(state = photos) { } /*!*/"),

        // And beside FEATURE, DATA and the rest in the companion:
        const val SHEET = "core/designsystem/src/main/kotlin/com/acme/core/designsystem/component/sheet/A.kt"
