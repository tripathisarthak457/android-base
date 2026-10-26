package com.example.cmp.core.notification

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project
// builds with. 35 is above Android 13, where posting notifications became a runtime permission.
internal const val ROBOLECTRIC_SDK = 35
