package com.example.everything.buildlogic.android

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
