package com.base.app.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleGraphTest {

    @Test
    fun `a feature reaching for another feature names the edge`() {
        assertEquals(
            listOf(":feature:trips -> :feature:orders"),
            violations(":feature:trips", ":core:ui", ":data:trips", ":feature:orders"),
        )
    }

    @Test
    fun `a data module may not see another data module`() {
        assertEquals(
            listOf(":data:trips -> :data:orders"),
            violations(":data:trips", ":core:network", ":data:orders"),
        )
    }

    @Test
    fun `core never sees data or features`() {
        assertEquals(
            listOf(":core:ui -> :data:orders", ":core:ui -> :feature:orders"),
            violations(":core:ui", ":core:designsystem", ":data:orders", ":feature:orders"),
        )
    }

    @Test
    fun `core common is the only pure module`() {
        assertTrue(ModuleShape.of(":core:common")!!.kind.pure)
        listOf(":core:ui", ":feature:orders", ":data:orders").forEach { path ->
            assertTrue(path, !ModuleShape.of(path)!!.kind.pure)
        }
    }

    @Test
    fun `the composition roots are not checked`() {
        assertNull(ModuleShape.of(":androidApp"))
        assertNull(ModuleShape.of(":shared"))
    }

    private fun violations(module: String, vararg dependencies: String): List<String> =
        ModuleShape.of(module)!!.violations(module, dependencies.toList())
}
