package com.base.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.designsystem.animation.AppAppear
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.input.AppTextArea
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.MviScreen
// <opt:coil>
import com.base.app.core.ui.AppNetworkImage
// </opt:coil>
// <opt:media>
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.media.rememberMediaPicker
// </opt:media>

@Composable
fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    MviScreen(viewModel = viewModel) { state, onEvent ->
        ProfileScreen(state = state, onEvent = onEvent)
    }
}

@Composable
fun ProfileScreen(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = state.saved.name.ifBlank { stringResource(R.string.profile_unnamed) }

    AppScaffold(
        modifier = modifier,
        topBar = { AppLargeTitle(title = stringResource(R.string.profile_title)) },
    ) {
        // Nothing is drawn until the stored profile has been read, so the fields never flash
        // empty and then fill in.
        if (!state.loaded) return@AppScaffold

        AppAppear {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.spacing.gutter),
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
                        style = AppTheme.typography.headingSmall,
                        color = AppTheme.colors.contentPrimary,
                        textAlign = TextAlign.Center,
                    )
                    // <opt:media>
                    PhotoActions(hasPhoto = state.saved.photoUri != null, onEvent = onEvent)
                    // </opt:media>
                }

                AppTextField(
                    value = state.name,
                    onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
                    label = stringResource(R.string.profile_name),
                    placeholder = stringResource(R.string.profile_name_placeholder),
                    error = if (state.nameError) stringResource(R.string.profile_name_required) else null,
                    maxLength = ProfileViewModel.NAME_LIMIT,
                )
                AppTextArea(
                    value = state.bio,
                    onValueChange = { onEvent(ProfileEvent.BioChanged(it)) },
                    label = stringResource(R.string.profile_bio),
                    placeholder = stringResource(R.string.profile_bio_placeholder),
                    maxLength = ProfileViewModel.BIO_LIMIT,
                )
                AppButton(
                    text = stringResource(R.string.profile_save),
                    onClick = { onEvent(ProfileEvent.Save) },
                    enabled = state.isDirty,
                    fillWidth = true,
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
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Small,
        )
        if (hasPhoto) {
            AppButton(
                text = stringResource(R.string.profile_remove_photo),
                onClick = { onEvent(ProfileEvent.PhotoPicked(null)) },
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Small,
            )
        }
    }
}
// </opt:media>

private val AvatarSize = 88.dp
