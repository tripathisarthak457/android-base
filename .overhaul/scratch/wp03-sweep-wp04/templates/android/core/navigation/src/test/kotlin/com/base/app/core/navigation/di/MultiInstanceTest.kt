package com.base.app.core.navigation.di

import androidx.activity.ComponentActivity
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.PendingDeepLink
import com.base.app.core.navigation.ROBOLECTRIC_SDK
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Two windows of the app (multi-instance on a tablet, two desktop windows) navigate on their own:
 * each activity gets its own navigator, links and registry, and keeps them through a
 * configuration change, as the Navigation 3 Hilt recipe scopes them.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], application = HiltTestApplication::class)
class MultiInstanceTest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun `each window has its own navigation`() {
        val first = Robolectric.buildActivity(WindowActivity::class.java).setup().get()
        val second = Robolectric.buildActivity(WindowActivity::class.java).setup().get()

        assertNotSame(first.navigator, second.navigator)
        assertNotSame(first.pendingDeepLink, second.pendingDeepLink)
        assertNotSame(first.registry, second.registry)
    }

    @Test
    fun `a window keeps its navigation through a configuration change`() {
        val window = Robolectric.buildActivity(WindowActivity::class.java).setup()
        val navigator = window.get().navigator
        val pending = window.get().pendingDeepLink
        val serialization = window.get().serialization

        window.recreate()

        assertSame(navigator, window.get().navigator)
        assertSame(pending, window.get().pendingDeepLink)
        assertSame(serialization, window.get().serialization)
    }
}

@AndroidEntryPoint
class WindowActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: AppNavigator

    @Inject
    lateinit var pendingDeepLink: PendingDeepLink

    @Inject
    lateinit var registry: NavRegistry

    @Inject
    lateinit var serialization: NavKeySerialization
}
