package com.base.app.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.base.app.core.designsystem.component.input.AppCurrencyField
import com.base.app.core.designsystem.component.input.AppFieldButton
import com.base.app.core.designsystem.component.input.AppOtpField
import com.base.app.core.designsystem.component.input.AppPhoneField
import com.base.app.core.designsystem.component.input.AppSelectField
import com.base.app.core.designsystem.component.input.AppStepper
import com.base.app.core.designsystem.component.input.AppTagInput
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The fields that exist because a plain text box gets them wrong.
 *
 * Every one is live: typing into the OTP boxes advances them, the currency field reformats as
 * digits arrive, the stepper clamps at its range. A catalog of screenshots would not show any of
 * that, which is the reason this is an app rather than a document.
 */
@Composable
fun FieldsSection() {
    var otp by remember { mutableStateOf("") }
    var otpFilled by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(1) }
    var country by remember { mutableStateOf<String?>(null) }
    var dialCode by remember { mutableStateOf("+91") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableLongStateOf(125_000L) }
    var tags by remember { mutableStateOf(listOf("compose", "android")) }
    var reminder by remember { mutableStateOf<String?>(null) }

    CatalogGroup(
        title = "One-time code",
        caption = "One hidden field drives every box, so paste and autofill work.",
    ) {
        AppOtpField(
            value = otp,
            onValueChange = { otp = it },
            length = 6,
            autoFocus = false,
            label = "Verification code",
            supporting = if (otpFilled) "Complete" else "Sent to +91 98765 43210",
            onFilled = { otpFilled = true },
        )
    }

    CatalogGroup(title = "Stepper", caption = "Clamped to 1..10; the arrows disable at the ends.") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppStepper(
                value = quantity,
                onValueChange = { quantity = it },
                range = 1..10,
            )
            AppText(
                text = "Quantity: $quantity",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentSecondary,
            )
        }
    }

    CatalogGroup(
        title = "Select",
        caption = "Read-only rather than disabled, so it stays focusable and announced.",
    ) {
        AppSelectField(
            value = country,
            options = listOf("India", "Singapore", "United Kingdom", "United States"),
            onSelect = { country = it },
            label = "Country",
            helper = "Where the account is billed",
        )
    }

    CatalogGroup(title = "Phone", caption = "Dial code and number are separate values.") {
        AppPhoneField(
            dialCode = dialCode,
            number = phone,
            onDialCodeChange = { dialCode = it },
            onNumberChange = { phone = it },
            helper = "We only use this to verify your account",
        )
    }

    CatalogGroup(
        title = "Currency",
        caption = "Held in minor units, so no amount is ever a rounded float.",
    ) {
        AppCurrencyField(
            minorUnits = amount,
            onValueChange = { amount = it },
            label = "Amount",
            helper = "Stored as $amount paise",
        )
    }

    CatalogGroup(title = "Tags", caption = "Enter or comma commits; backspace on empty removes.") {
        AppTagInput(
            tags = tags,
            onTagsChange = { tags = it },
            label = "Topics",
            helper = "Up to 10",
        )
    }

    CatalogGroup(
        title = "Field button",
        caption = "For a value chosen in a dialog — a date, a file, an address.",
    ) {
        AppFieldButton(
            label = "Reminder",
            value = reminder,
            onClick = { reminder = if (reminder == null) "Tomorrow, 09:00" else null },
            placeholder = "Tap to set",
            icon = AppIcons.Bell,
        )
    }
}
