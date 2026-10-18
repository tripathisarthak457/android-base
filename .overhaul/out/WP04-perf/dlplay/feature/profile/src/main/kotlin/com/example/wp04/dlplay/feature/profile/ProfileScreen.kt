package com.example.wp04.dlplay.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.wp04.dlplay.core.designsystem.animation.AppAppear
import com.example.wp04.dlplay.core.designsystem.component.button.AppButton
import com.example.wp04.dlplay.core.designsystem.component.container.AppScaffold
import com.example.wp04.dlplay.core.designsystem.component.feedback.AppAvatar
import com.example.wp04.dlplay.core.designsystem.component.input.AppTextArea
import com.example.wp04.dlplay.core.designsystem.component.input.AppTextField
import com.example.wp04.dlplay.core.designsystem.component.navigation.AppLargeTitle
import com.example.wp04.dlplay.core.designsystem.component.text.AppText
import com.example.wp04.dlplay.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.wp04.dlplay.core.designsystem.foundation.layout.chromeContentPadding
import com.example.wp04.dlplay.core.designsystem.theme.AppTheme
import com.example.wp04.dlplay.core.ui.MviScreen
import com.example.wp04.dlplay.core.ui.AppNetworkImage
import com.example.wp04.dlplay.core.designsystem.component.button.ButtonSize
import com.example.wp04.dlplay.core.designsystem.component.button.ButtonVariant
import com.example.wp04.dlplay.core.media.rememberMediaPicker

@Composable
internal fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { viewModel.onEvent(ProfileEvent.Appeared) }

    MviScreen(viewModel = viewModel) { state, onEvent ->
        ProfileScreen(state = state, name = viewModel.name, bio = viewModel.bio, onEvent = onEvent)
    }
}

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

    AppScaffold(
        contentMaxWidth = AppTheme.layout.formMaxWidth,
        modifier = modifier,
        topBar = { AppLargeTitle(title = stringResource(R.string.profile_title)) },
    ) {
        // Nothing is drawn until the stored profile has been read, so the fields never flash
        // empty and then fill in.
        if (!state.loaded) return@AppScaffold

        AppAppear {
            val scroll = rememberScrollState()
            ReportScrollEdge(scroll)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
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
                        content = state.saved.photoUri?.let { uri ->
                            { AppNetworkImage(url = uri, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                        },
                    )
                    AppText(
                        text = displayName,
                        style = AppTheme.typography.headingSmall,
                        color = AppTheme.colors.contentPrimary,
                        textAlign = TextAlign.Center,
                    )
                    PhotoActions(hasPhoto = state.saved.photoUri != null, onEvent = onEvent)
                }

                AppTextField(
                    value = name.text.toString(),
                    onValueChange = name::setTextAndPlaceCursorAtEnd,
                    label = stringResource(R.string.profile_name),
                    placeholder = stringResource(R.string.profile_name_placeholder),
                    error = if (state.nameError) stringResource(R.string.profile_name_required) else null,
                    maxLength = ProfileViewModel.NAME_LIMIT,
                )
                AppTextArea(
                    value = bio.text.toString(),
                    onValueChange = bio::setTextAndPlaceCursorAtEnd,
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

private val AvatarSize = 88.dp
