package com.base.app.deeplink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.base.app.MainActivity
import com.base.app.core.navigation.PendingDeepLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Where a link lands, so MainActivity can stay singleTop. The link is held here, and MainActivity is
 * started exactly as the launcher starts it: a task a link opened then has the launcher's intent at
 * its root, and the icon tapped later brings that task back instead of stacking a second copy of the
 * app on top of it. CLEAR_TOP with SINGLE_TOP brings a running instance forward; it opens the held
 * link once the tabs are up, as it does for one that arrived before sign-in.
 */
@AndroidEntryPoint
class LinkActivity : ComponentActivity() {

    @Inject
    lateinit var pendingDeepLink: PendingDeepLink

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLink.hold(intent?.data)
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
