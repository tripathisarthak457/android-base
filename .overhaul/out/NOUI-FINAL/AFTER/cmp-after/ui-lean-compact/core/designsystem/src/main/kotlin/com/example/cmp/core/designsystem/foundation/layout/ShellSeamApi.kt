package com.example.cmp.core.designsystem.foundation.layout

/**
 * Marks the seams between the design system and the app's shell: what the navigation hosts and the
 * liquid glass feature provide to sheets, and what sheets report back. Features never need them;
 * they use `AppSheet`, and the build's code rules reject this opt-in under `feature/`.
 */
@RequiresOptIn("A shell seam: features use AppSheet", RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ShellSeamApi
