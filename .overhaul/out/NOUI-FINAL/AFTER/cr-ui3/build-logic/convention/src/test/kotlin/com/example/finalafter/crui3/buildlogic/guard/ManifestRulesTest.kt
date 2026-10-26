package com.example.finalafter.crui3.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestRulesTest {

    @Test
    fun `an adaptive manifest passes`() {
        assertEquals(emptyList<String>(), ManifestRules.violations(manifest()))
    }

    @Test
    fun `a launcher activity recreated on resize names the changes it misses`() {
        val problems = ManifestRules.violations(manifest(configChanges = "orientation|screenSize"))

        assertEquals(
            listOf(
                "<activity com.acme.MainActivity> is recreated on smallestScreenSize, screenLayout, keyboard, " +
                    "keyboardHidden, navigation; add them to android:configChanges",
            ),
            problems,
        )
    }

    @Test
    fun `locks, missing flags and an unexported component are each reported`() {
        val problems = ManifestRules.violations(
            manifest(
                back = false,
                multiInstance = false,
                extra = """
                    <activity android:name="com.lib.Locked" android:exported="false" android:screenOrientation="portrait"
                        android:resizeableActivity="false" />
                    <service android:name="com.lib.Sync" />
                    <property android:name="android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY" android:value="true" />
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "<application> lacks android:enableOnBackInvokedCallback=\"true\", which predictive back needs",
                "<application> lacks <property android:name=\"android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI\" " +
                    "android:value=\"true\" />",
                "android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY opts out of resizing, which Android 17 no " +
                    "longer allows",
                "<activity com.lib.Locked> sets android:screenOrientation",
                "<activity com.lib.Locked> sets android:resizeableActivity=\"false\"",
                "<service com.lib.Sync> does not say whether it is exported",
            ),
            problems,
        )
    }

    @Test
    fun `a library component on the allow list is let through, and an entry needs a reason`() {
        val allowList = ManifestRules.allowList("com.lib.Sync  # the library declares it before Android 12\ncom.lib.Other")

        assertEquals(listOf("com.lib.Other"), allowList.unexplained)
        assertEquals(
            emptyList<String>(),
            ManifestRules.violations(manifest(extra = "<service android:name=\"com.lib.Sync\" />"), allowList.names),
        )
    }

    private fun manifest(
        configChanges: String = ManifestRules.CONFIG_CHANGES.joinToString("|"),
        back: Boolean = true,
        multiInstance: Boolean = true,
        extra: String = "",
    ): String = """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.acme">
            <application ${if (back) "android:enableOnBackInvokedCallback=\"true\"" else ""}>
                ${if (multiInstance) "<property android:name=\"android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI\" android:value=\"true\" />" else ""}
                <activity android:name="com.acme.MainActivity" android:exported="true" android:configChanges="$configChanges">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
                $extra
            </application>
        </manifest>
    """.trimIndent()
}
