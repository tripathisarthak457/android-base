package com.base.app.core.navigation

/**
 * Marks what a navigation host needs from the runtime and a feature never does: taking the
 * navigator's commands and applying them to a back stack, a stack's entries and its
 * [back gate][com.base.app.core.navigation.back.BackGate], the rules a command is applied by, and
 * the destination a key renders. A host (the shell in `:core:navigation:shell`, or
 * one a project writes) opts in for its whole module; a feature registers with `navGraph { }`,
 * navigates through [AppNavigator] and never opts in.
 */
@RequiresOptIn(
    "Navigation host API: a feature navigates through AppNavigator",
    RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
annotation class NavigationHostApi
