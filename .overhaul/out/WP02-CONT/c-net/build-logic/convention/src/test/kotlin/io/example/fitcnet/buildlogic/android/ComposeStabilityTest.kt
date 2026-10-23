package io.example.fitcnet.buildlogic.android

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
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
    fun `the check reads the main compilation's report as it ended, whatever compiles after it`() {
        val root = ProjectBuilder.builder().withProjectDir(folder.root).build()
        val core = ProjectBuilder.builder().withName("core").withParent(root).build()
        val module = ProjectBuilder.builder().withName("designsystem").withParent(core).build()
        val compile = module.tasks.register("compileDebugKotlin").get()
        module.configureComposeStability()
        val written = module.layout.buildDirectory.dir("compose_reports").get().asFile.apply { mkdirs() }

        written.resolve("designsystem-composables.txt").writeText("main")
        written.resolve("designsystem-classes.txt").writeText("classes")
        compile.actions.forEach { it.execute(compile) }
        // A test compilation takes the module's name, so it writes over the same file afterwards.
        written.resolve("designsystem-composables.txt").writeText("tests")

        val check = module.tasks.getByName("checkComposeStability") as CheckComposeStabilityTask
        assertEquals(listOf("main"), check.reports.files.map { it.readText() })
        val record = module.tasks.getByName("recordComposeStability") as RecordComposeStabilityTask
        assertEquals(listOf("main"), record.reports.files.map { it.readText() })
    }

    @Test
    fun `a report the main compilation no longer writes is not read again`() {
        val from = folder.newFolder("reports").apply { resolve("old-composables.txt").writeText("old") }
        val to = folder.newFolder("stability")

        copyReports(from, to)
        from.resolve("old-composables.txt").delete()
        from.resolve("designsystem-composables.txt").writeText("new")
        copyReports(from, to)

        assertEquals(listOf("designsystem-composables.txt"), to.list()?.toList())
    }
}
