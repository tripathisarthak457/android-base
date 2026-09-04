package com.base.app.core.designsystem.component.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.overlay.AppDropdownMenu
import com.base.app.core.designsystem.component.overlay.AppMenuItem
import com.base.app.core.designsystem.component.selection.AppChip
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A quantity stepper.
 *
 * Bounds are enforced here rather than by the caller, and the button at a bound is disabled
 * rather than hidden — a control that disappears at the limit makes the layout jump and leaves
 * the user unsure whether they hit a maximum or the app broke.
 */
@Composable
fun AppStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
    step: Int = 1,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val canDecrease = enabled && value - step >= range.first
    val canIncrease = enabled && value + step <= range.last

    Row(
        modifier = modifier
            .disabledAlpha(enabled)
            .background(colors.surfaceVariant, AppTheme.shapes.pill)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = AppIcons.Minus,
            contentDescription = "Decrease",
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = canDecrease,
            size = ButtonSize.Small,
            shape = AppTheme.shapes.pill,
        )
        AppText(
            text = value.toString(),
            modifier = Modifier
                .defaultMinSize(minWidth = 40.dp)
                .padding(horizontal = AppTheme.spacing.xs),
            style = AppTheme.typography.titleMedium,
            color = colors.contentPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        AppIconButton(
            icon = AppIcons.Plus,
            contentDescription = "Increase",
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = canIncrease,
            size = ButtonSize.Small,
            shape = AppTheme.shapes.pill,
        )
    }
}

/**
 * A field that opens a menu instead of a keyboard.
 *
 * Read-only rather than disabled: a disabled field is dimmed and skipped by accessibility
 * traversal, which is wrong for a control the user is expected to operate. Read-only keeps it
 * focusable and announced, and simply refuses text.
 */
@Composable
fun <T> AppSelectField(
    value: T?,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "Select",
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AppTextField(
            value = value?.let(optionLabel).orEmpty(),
            onValueChange = {},
            label = label,
            placeholder = placeholder,
            helper = helper,
            error = error,
            enabled = enabled,
            readOnly = true,
            trailing = {
                AppIcon(
                    imageVector = if (expanded) AppIcons.ChevronUp else AppIcons.ChevronDown,
                    contentDescription = null,
                    tint = AppTheme.colors.contentTertiary,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                // The tap target is the whole field, not just the chevron. A dropdown whose only
                // affordance is a 20dp glyph is one people miss.
                .appClickable(
                    onClick = { expanded = true },
                    enabled = enabled,
                    role = Role.DropdownList,
                    minTouchTarget = 0.dp,
                ),
        )

        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                AppMenuItem(
                    text = optionLabel(option),
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailing = if (option == value) {
                        {
                            AppIcon(
                                AppIcons.Check,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                size = 16.dp,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/**
 * A phone number, split into a dialling code and the rest.
 *
 * Two controls rather than one free-text field, because a number stored with the country code
 * inconsistently attached is the single most common cause of "we sent the code and it never
 * arrived". [onValueChange] reports them separately so the caller stores both.
 */
@Composable
fun AppPhoneField(
    dialCode: String,
    number: String,
    onDialCodeChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    dialCodes: List<String> = DefaultDialCodes,
    label: String? = "Phone number",
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    maxLength: Int = 15,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    // One label above a Row of two bare fields, rather than a label on each. Two labelled fields
    // side by side either read as two separate questions or need a blank label on one to keep the
    // baselines aligned — and a blank label is a screen reader announcing nothing.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        label?.let {
            AppText(
                text = it,
                style = AppTheme.typography.titleSmall,
                color = colors.contentSecondary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A fixed width, not a minimum. AppTextField fills the width it is given, and an
            // unweighted child that fills the width takes the whole Row — leaving the weighted
            // number field beside it measuring zero and rendering as nothing at all.
            Box(modifier = Modifier.width(DIAL_CODE_WIDTH)) {
                AppTextField(
                    value = dialCode,
                    onValueChange = {},
                    enabled = enabled,
                    readOnly = true,
                    modifier = Modifier
                        .appClickable(
                            onClick = { expanded = true },
                            enabled = enabled,
                            role = Role.DropdownList,
                            minTouchTarget = 0.dp,
                        ),
                    trailing = {
                        AppIcon(
                            AppIcons.ChevronDown,
                            contentDescription = "Change country code",
                            tint = colors.contentTertiary,
                            size = 16.dp,
                        )
                    },
                )
                AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    dialCodes.forEach { code ->
                        AppMenuItem(
                            text = code,
                            onClick = {
                                expanded = false
                                onDialCodeChange(code)
                            },
                        )
                    }
                }
            }

            AppNumberField(
                value = number,
                onValueChange = onNumberChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                maxLength = maxLength,
                imeAction = ImeAction.Done,
            )
        }

        (error ?: helper)?.let {
            AppText(
                text = it,
                style = AppTheme.typography.caption,
                color = if (error != null) colors.danger.content else colors.contentTertiary,
            )
        }
    }
}

/** A short default list. Replace it with the codes your product actually serves. */
val DefaultDialCodes: List<String> = listOf("+1", "+44", "+61", "+65", "+91", "+971")

/** Wide enough for a four-digit code and the chevron. */
private val DIAL_CODE_WIDTH = 108.dp

/**
 * A money field that groups digits as they are typed.
 *
 * The value is carried as **minor units** — paise, cents — not as a formatted string and not as a
 * `Double`. Storing money in a float is how a total ends up at 19.999999999999998, and re-parsing
 * a formatted string on every keystroke is how a group separator ends up inside the value.
 */
@Composable
fun AppCurrencyField(
    minorUnits: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    symbol: String = "₹",
    fractionDigits: Int = 2,
    label: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    maxMinorUnits: Long = 99_999_999_99L,
) {
    AppTextField(
        value = formatMinorUnits(minorUnits, fractionDigits),
        onValueChange = { typed ->
            val digits = typed.filter(Char::isDigit).take(MAX_DIGITS)
            onValueChange((digits.toLongOrNull() ?: 0L).coerceAtMost(maxMinorUnits))
        },
        modifier = modifier,
        label = label,
        placeholder = formatMinorUnits(0, fractionDigits),
        helper = helper,
        error = error,
        enabled = enabled,
        singleLine = true,
        leadingIcon = null,
        trailing = {
            AppText(
                text = symbol,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.contentTertiary,
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions.Default,
    )
}

/**
 * `123456` → `1,234.56`.
 *
 * Grouping is done by hand rather than through `NumberFormat`, because the field has to render a
 * partially typed value — and a locale formatter given "1" while the user is mid-entry will
 * happily produce something the next keystroke cannot be appended to.
 */
private fun formatMinorUnits(minorUnits: Long, fractionDigits: Int): String {
    val text = minorUnits.toString().padStart(fractionDigits + 1, '0')
    val whole = text.dropLast(fractionDigits).ifEmpty { "0" }
    val fraction = text.takeLast(fractionDigits)

    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    return if (fractionDigits == 0) grouped else "$grouped.$fraction"
}

private const val MAX_DIGITS = 12

/**
 * A field that turns what you type into removable chips.
 *
 * Commas and the Done key both commit, because people reach for either. Duplicates are rejected
 * silently rather than with an error — the tag is already there, which is what the user wanted.
 */
@Composable
fun AppTagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "Add a tag",
    helper: String? = null,
    enabled: Boolean = true,
    maxTags: Int = 10,
) {
    var draft by remember { mutableStateOf("") }
    val colors = AppTheme.colors

    fun commit() {
        val candidate = draft.trim().trimEnd(',')
        draft = ""
        if (candidate.isBlank() || candidate in tags || tags.size >= maxTags) return
        onTagsChange(tags + candidate)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppTextField(
            value = draft,
            onValueChange = { typed ->
                if (typed.endsWith(",")) {
                    draft = typed
                    commit()
                } else {
                    draft = typed
                }
            },
            label = label,
            placeholder = if (tags.size >= maxTags) "Limit reached" else placeholder,
            helper = helper ?: "${tags.size} of $maxTags",
            enabled = enabled && tags.size < maxTags,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            trailing = if (draft.isNotBlank()) {
                {
                    AppIconButton(
                        icon = AppIcons.Plus,
                        contentDescription = "Add tag",
                        onClick = { commit() },
                        size = ButtonSize.Small,
                    )
                }
            } else {
                null
            },
        )

        if (tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                tags.forEach { tag ->
                    AppChip(
                        label = tag,
                        onClick = {},
                        selected = true,
                        enabled = enabled,
                        onRemove = { onTagsChange(tags - tag) },
                    )
                }
            }
        }
    }
}

/**
 * A labelled control with an icon, for a settings row that opens something.
 *
 * Not a list item: this is a *field*, so it lines up with the text fields above and below it in a
 * form rather than spanning to the window edges the way a list row does.
 */
@Composable
fun AppFieldButton(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Not set",
    icon: ImageVector? = null,
    enabled: Boolean = true,
    error: String? = null,
) {
    val colors = AppTheme.colors
    val borderColor by animateColorAsState(
        targetValue = if (error != null) colors.danger.content else colors.border,
        animationSpec = tween(AppTheme.motion.quick),
        label = "fieldButtonBorder",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.titleSmall,
            color = colors.contentSecondary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .disabledAlpha(enabled)
                .background(colors.surface, AppTheme.shapes.sm)
                .border(AppTheme.sizes.borderWidth, borderColor, AppTheme.shapes.sm)
                .appClickable(onClick = onClick, enabled = enabled, minTouchTarget = 0.dp)
                .defaultMinSize(minHeight = AppTheme.sizes.fieldHeight)
                .padding(horizontal = AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { AppIcon(it, contentDescription = null, tint = colors.contentTertiary) }
            AppText(
                text = value ?: placeholder,
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.bodyMedium,
                color = if (value == null) colors.contentTertiary else colors.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppIcon(AppIcons.ChevronRight, contentDescription = null, tint = colors.contentTertiary)
        }
        error?.let {
            AppText(text = it, style = AppTheme.typography.caption, color = colors.danger.content)
        }
    }
}
