package com.example.cmp.deeplink

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.cmp.MainActivity

/**
 * Where a link lands, so MainActivity can stay singleTop. MainActivity is started exactly as the
 * launcher starts it, with the link as an extra: a task a link opened then has the launcher's
 * intent at its root (extras do not count when Android compares intents), and the icon tapped
 * later brings that task back instead of stacking a second copy of the app on top of it.
 * CLEAR_TOP with SINGLE_TOP brings a running instance forward; MainActivity holds the link and
 * the app's stack opens it once start-up has read the settings and the session.
 */
class LinkActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .putExtra(MainActivity.EXTRA_LINK, intent?.dataString)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
        )
        finish()
    }
}
