package com.base.app.buildlogic.android

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ComposeStabilityTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `an unstable parameter is named with its function, generic functions included`() {
        val report = """
            restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun com.acme.ds.Foo(
              unstable items: MutableList<String>
              stable modifier: Modifier? = @static Companion
            )
            restartable skippable scheme("[androidx.compose.ui.UiComposable]") fun <T> com.acme.ds.AppPicker(
              stable value: T
              unstable options: ArrayList<T>
            )
            restartable skippable fun com.acme.ds.Clean(
              stable text: String
            )
        """.trimIndent()

        assertEquals(
            listOf("com.acme.ds.AppPicker(options)", "com.acme.ds.Foo(items)"),
            ComposeStability.problems(listOf(report)),
        )
    }

    @Test
    fun `a restartable function that cannot skip is reported, since only changed flags allow it`() {
        val report = """
            restartable scheme("[androidx.compose.ui.UiComposable]") fun com.acme.ds.Stuck(
              stable text: String
            )
        """.trimIndent()

        assertEquals(listOf("com.acme.ds.Stuck: not skippable"), ComposeStability.problems(listOf(report)))
    }

    @Test
    fun `the baseline accepts a parameter only with a reason`() {
        val baseline = ComposeStability.baseline(
            """
            # header
            com.acme.ds.Foo(items)  # callers pass the row's own list
            com.acme.ds.Bar(key)
            """.trimIndent(),
        )

        assertEquals(setOf("com.acme.ds.Foo(items)", "com.acme.ds.Bar(key)"), baseline.accepted)
        assertEquals(listOf("com.acme.ds.Bar(key)"), baseline.unexplained)
    }

    @Test
    fun `an entry for a declaration of this module that the report no longer flags is stale`() {
        val baseline = ComposeStability.baseline(
            """
            com.acme.ds.Foo(items)  # callers pass the row's own list
            com.acme.ds.list.AppPicker(options)  # fixed since
            com.acme.ds.Stuck: not skippable  # fixed since
            com.acme.ds.Sheet.Content(state)  # a member, fixed since
            com.acme.ds.rememberRow(key)  # fixed since
            com.acme.nav.BackEffect(handler)  # another module's
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "com.acme.ds.Sheet.Content(state)",
                "com.acme.ds.Stuck: not skippable",
                "com.acme.ds.list.AppPicker(options)",
                "com.acme.ds.rememberRow(key)",
            ),
            ComposeStability.stale(baseline, listOf("com.acme.ds.Foo(items)"), setOf("com.acme.ds", "com.acme.ds.list")),
        )
    }

    @Test
    fun `the check fails on a stale entry and names it`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val report = folder.newFile("designsystem-composables.txt").apply {
            writeText(
                """
                restartable skippable fun com.acme.ds.Foo(
                  unstable items: MutableList<String>
                )
                """.trimIndent(),
            )
        }
        val source = folder.newFile("Foo.kt").apply { writeText("package com.acme.ds\n\n@Composable fun Foo(items: MutableList<String>) {}\n") }
        val baseline = folder.newFile("compose-stability-baseline.txt").apply {
            writeText("com.acme.ds.Foo(items)  # callers pass the row's own list\ncom.acme.ds.Bar(key)  # fixed since\n")
        }
        val check = root.tasks.register("checkComposeStability", CheckComposeStabilityTask::class.java).get().apply {
            reports.from(report)
            sources.from(source)
            baselineFile.set(baseline)
            modulePath.set(":core:designsystem")
            marker.set(folder.root.resolve("ok"))
        }

        val error = assertThrows(GradleException::class.java) { check.verify() }

        assertTrue(error.message!!, "  com.acme.ds.Bar(key)" in error.message!!.lines())
        assertTrue(error.message!!, "com.acme.ds.Foo(items)" !in error.message!!)
    }

    @Test
    fun `the shared modules and every feature are checked, other modules only when asked`() {
        val checked = listOf(
            ":core:designsystem", ":core:ui", ":core:navigation", ":core:navigation:shell",
            ":feature:orders", ":feature:orders:impl",
        )
        val unchecked = listOf(":feature:orders:api", ":core:network", ":data:orders", ":app", ":catalog")

        assertEquals(checked, checked.filter(::isStabilityChecked))
        assertEquals(emptyList<String>(), unchecked.filter(::isStabilityChecked))
    }

    @Test
    fun `the module's other compilations write their report only after the check has read this one`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val core = ProjectBuilder.builder().withName("core").withParent(root).build()
        val module = ProjectBuilder.builder().withName("designsystem").withParent(core).build()
        val compiles = listOf(
            "compileDebugKotlin", "compileReleaseKotlin", "compileDebugUnitTestKotlin",
            "compileReleaseUnitTestKotlin", "compileDebugAndroidTestKotlin",
        )
        (compiles + listOf("kspDebugKotlin", "compileDebugJavaWithJavac")).forEach { module.tasks.register(it) }

        module.configureComposeStability()

        val readers = setOf(module.tasks.getByName("checkComposeStability"), module.tasks.getByName("recordComposeStability"))
        fun after(name: String) = module.tasks.getByName(name).mustRunAfter.getDependencies(null)
        assertEquals(emptySet<Any>(), after("compileDebugKotlin"))
        compiles.drop(1).forEach { assertEquals(it, readers, after(it)) }
        assertEquals(emptySet<Any>(), after("kspDebugKotlin"))
        assertEquals(emptySet<Any>(), after("compileDebugJavaWithJavac"))
    }
}
