package com.example.standard.catalog.sections

import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.standard.catalog.CatalogGroup
import com.example.standard.core.designsystem.component.input.AppSearchField
import com.example.standard.core.designsystem.component.input.AppSecureField
import com.example.standard.core.designsystem.component.input.AppTextField
import com.example.standard.core.designsystem.icon.AppIcons

@Composable
fun InputsSection() {
    CatalogGroup("Text field", caption = "Each field reads and writes a TextFieldState; in an app the ViewModel owns it.") {
        AppTextField(state = rememberTextFieldState(), label = "Full name", placeholder = "Ada Lovelace", helper = "As it appears on your card")
        AppTextField(state = rememberTextFieldState("not-an-email"), label = "Email", leadingIcon = AppIcons.Mail, error = "Enter a valid email address")
        AppTextField(state = rememberTextFieldState(), label = "Amount", prefix = "$", suffix = "USD", placeholder = "0.00")
        AppTextField(state = rememberTextFieldState("Read only"), label = "Account", readOnly = true)
        AppTextField(state = rememberTextFieldState("Disabled"), label = "Plan", enabled = false)
    }
    CatalogGroup("Multi-line and limits", caption = "A limit refuses input past it, a paste included; the counter shows how much is left.") {
        AppTextField(
            state = rememberTextFieldState(),
            label = "Note",
            placeholder = "Anything we should know?",
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6),
            maxLength = 140,
            showCounter = true,
        )
    }
    CatalogGroup("Secure field", caption = "The platform's secure field: kept off the clipboard, last character shown briefly.") {
        AppSecureField(state = rememberTextFieldState(), label = "Password", helper = "At least 8 characters")
    }
    CatalogGroup("Search", caption = "Cancel appears while it has focus; scopes appear while searching.") {
        var scope by remember { mutableIntStateOf(0) }
        AppSearchField(state = rememberTextFieldState(), onCancel = {}, scopes = listOf("All", "People", "Posts"), selectedScope = scope, onScopeChange = { scope = it })
    }
}
