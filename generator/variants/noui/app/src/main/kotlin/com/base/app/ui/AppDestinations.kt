package com.base.app.ui

import com.base.app.core.common.navigation.AppNavKey
import com.base.app.ui.start.StartKey

/** The app's shape, in one file. */
object AppDestinations {

    /**
     * Where every launch lands: a blank screen in the theme, until the project has screens of its
     * own. Point it at the first one, or choose per identity in `AppRoot`, where the stack is keyed
     * on who the app is for (onboarding on a first launch, sign-in when signed out).
     */
    val start: AppNavKey = StartKey
}
