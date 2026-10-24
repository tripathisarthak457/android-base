package com.example.everything.core.testing.compose

/**
 * The SDK Robolectric renders at: `@Config(sdk = [TEST_SDK])`. It trails the app's target because
 * Robolectric refuses the newest levels on the JDKs it supports.
 */
const val TEST_SDK = 35
