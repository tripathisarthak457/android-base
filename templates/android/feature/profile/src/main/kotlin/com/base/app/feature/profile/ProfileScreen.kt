package com.base.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.animation.AppAppear
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.layout.ReportScrollEdge
import com.base.app.core.designsystem.foundation.layout.chromeContentPadding
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.theme.AppTheme
// <opt:coil>
import com.base.app.core.ui.AppNetworkImage
// </opt:coil>
// <opt:media>
import androidx.compose.foundation.layout.Row
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.media.picker.rememberMediaPicker
// </opt:media>

/**
 * The design-system fields take a String, so each field's text is read from the ViewModel's
 * `TextFieldState` and written straight back; nothing is copied into the state.
 */
@Composable
internal fun ProfileScreen(
    state: ProfileState,
    name: TextFieldState,
    bio: TextFieldState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = state.saved.name.ifBlank { stringResource(R.string.profile_unnamed) }

    val scroll = rememberScrollState()
    AppScaffold(
        contentMaxWidth = AppTheme.layout.formMaxWidth,
        modifier = modifier,
        topBar = {
            AppLargeTitleTopBar(title = stringResource(R.string.profile_title), scrolled = { scroll.value })
        },
    ) {
        // Nothing is drawn until the stored profile has been read, so the fields never flash
        // empty and then fill in.
        if (!state.loaded) return@AppScaffold

        AppAppear {
            ReportScrollEdge(scroll)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())
                    .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.gutter))),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    AppAvatar(
                        name = displayName,
                        size = AvatarSize,
                        // <opt:coil>
                        content = state.saved.photoUri?.let { uri ->
                            { AppNetworkImage(url = uri, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                        },
                        // </opt:coil>
                    )
                    AppText(
                        text = displayName,
                        style = AppTheme.typography.headline,
                        color = AppTheme.colors.contentPrimary,
                        textAlign = TextAlign.Center,
                    )
                    // <opt:media>
                    PhotoActions(hasPhoto = state.saved.photoUri != null, onEvent = onEvent)
                    // </opt:media>
                }

                AppTextField(
                    state = name,
                    label = stringResource(R.string.profile_name),
                    placeholder = stringResource(R.string.profile_name_placeholder),
                    error = if (state.nameError) stringResource(R.string.profile_name_required) else null,
                    maxLength = ProfileViewModel.NAME_LIMIT,
                )
                AppTextField(
                    state = bio,
                    label = stringResource(R.string.profile_bio),
                    placeholder = stringResource(R.string.profile_bio_placeholder),
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 4, maxHeightInLines = 8),
                    maxLength = ProfileViewModel.BIO_LIMIT,
                )
                AppButton(
                    text = stringResource(R.string.profile_save),
                    onClick = { onEvent(ProfileEvent.Save) },
                    enabled = state.isDirty,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
// <opt:media>

@Composable
private fun PhotoActions(hasPhoto: Boolean, onEvent: (ProfileEvent) -> Unit) {
    val picker = rememberMediaPicker { uris ->
        uris.firstOrNull()?.let { onEvent(ProfileEvent.PhotoPicked(it.toString())) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        AppButton(
            text = stringResource(R.string.profile_change_photo),
            onClick = picker::pickImage,
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Small,
        )
        if (hasPhoto) {
            AppButton(
                text = stringResource(R.string.profile_remove_photo),
                onClick = { onEvent(ProfileEvent.PhotoPicked(null)) },
                variant = ButtonVariant.Plain,
                size = ButtonSize.Small,
            )
        }
    }
}
// </opt:media>

private val AvatarSize = 88.dp
