package com.example.cmp.buildlogic.guard

import com.example.cmp.buildlogic.guard.ApiPurity.Violation
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiPurityTest {

    @Test
    fun `a key may use core common, serialization and its own api package`() {
        val source = """
            package com.acme.app.feature.orders.api

            import com.acme.app.core.common.navigation.AppNavKey
            import com.acme.app.feature.orders.api.model.OrderFilter
            import kotlinx.serialization.Serializable
        """.trimIndent()

        assertEquals(emptyList<Violation>(), ApiPurity.violations(source))
    }

    @Test
    fun `android, compose and injection are refused`() {
        val source = """
            package com.acme.app.data.orders.api

            import android.net.Uri
            import androidx.compose.runtime.Immutable
            import javax.inject.Inject
            import dagger.Binds
        """.trimIndent()

        assertEquals(
            listOf(
                "android.net.Uri",
                "androidx.compose.runtime.Immutable",
                "javax.inject.Inject",
                "dagger.Binds",
            ),
            ApiPurity.violations(source).map { it.import },
        )
    }

    @Test
    fun `another module's internals and another domain's api are refused, with the line`() {
        val source = """
            package com.acme.app.feature.orders.api

            import com.acme.app.feature.orders.list.OrderListViewModel
            import com.acme.app.data.orders.api.OrderId
        """.trimIndent()

        assertEquals(
            listOf(
                Violation(3, "com.acme.app.feature.orders.list.OrderListViewModel"),
                Violation(4, "com.acme.app.data.orders.api.OrderId"),
            ),
            ApiPurity.violations(source),
        )
    }

    @Test
    fun `a shared key folder in core navigation is held to the same rule`() {
        val source = """
            package com.acme.app.feature.orders.api

            import com.acme.app.core.navigation.AppNavigator
        """.trimIndent()

        assertEquals(
            listOf("com.acme.app.core.navigation.AppNavigator"),
            ApiPurity.violations(source).map {
                it.import
            },
        )
    }

    @Test
    fun `files outside an api package are not this guard's business`() {
        val source = """
            package com.acme.app.feature.orders.list

            import android.net.Uri
        """.trimIndent()

        assertEquals(emptyList<Violation>(), ApiPurity.violations(source))
    }
}
