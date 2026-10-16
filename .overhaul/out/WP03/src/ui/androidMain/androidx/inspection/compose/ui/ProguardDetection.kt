package androidx.inspection.compose.ui;

/**
 * Purposely empty, unused class that would be removed by proguard.
 *
 * We use this class to detect if a target app was proguarded or not, because if it was,
 * it likely means we can't reliably inspect it, since library's methods used only by
 * an inspector would get removed. Instead, we'll need to tell users that app
 * inspection isn't available for the current app and that they should rebuild it
 * again without proguarding to continue.
 */
 private class ProguardDetection {}