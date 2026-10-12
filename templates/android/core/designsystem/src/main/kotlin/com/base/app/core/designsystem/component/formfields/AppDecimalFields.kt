package com.base.app.core.designsystem.component.formfields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.foundation.content.rememberCurrentLocale
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * A decimal quantity typed as text: litres, kilometres, a rate to four places. The value stays the
 * text the user typed, with a `.` for the point whatever the locale, because an API takes these as
 * decimal strings and a `Double` would turn `0.1` into something else on the way.
 *
 * Anything that is not a digit or the one point is refused as it is typed or pasted, as is a
 * keystroke past [maxDecimals] places or [maxIntegerDigits] digits. With [grouping], the locale's
 * separators are shown but never stored, and the caret stays where it was typed.
 */
@Composable
fun AppDecimalField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    maxDecimals: Int = 2,
    maxIntegerDigits: Int = 7,
    unit: String? = null,
    prefix: String? = null,
    grouping: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    val locale = rememberCurrentLocale()
    val input = remember(maxDecimals, maxIntegerDigits, locale) {
        DecimalInputTransformation(maxDecimals, maxIntegerDigits, DecimalFormatSymbols.getInstance(locale).decimalSeparator)
    }
    val output = remember(locale, grouping) { LocaleDecimalOutput(locale, grouping) }
    AppTextField(
        state = state,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helper = helper,
        error = error,
        enabled = enabled,
        prefix = prefix,
        suffix = unit,
        inputTransformation = input,
        outputTransformation = output,
        keyboardOptions = KeyboardOptions(keyboardType = if (maxDecimals > 0) KeyboardType.Decimal else KeyboardType.Number, imeAction = imeAction),
    )
}

/**
 * An amount of money in minor units (cents, paise), typed the way it is said: `99.5` is 99.50, not
 * 0.99 as a cash-register field would make it. The currency's symbol, decimal places and the
 * locale's grouping are applied. [minorUnits] is null while the field is empty; a value set from
 * outside (a form reset) replaces what is typed.
 */
@Composable
fun AppMoneyField(
    minorUnits: Long?,
    onMinorUnitsChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    currency: Currency = Currency.getInstance(rememberCurrentLocale()),
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    val locale = rememberCurrentLocale()
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    val state = rememberTextFieldState(minorUnits?.let { DecimalInput.fromMinor(it, digits) }.orEmpty())
    val onChange by rememberUpdatedState(onMinorUnitsChange)
    LaunchedEffect(minorUnits, digits) {
        if (DecimalInput.toMinor(state.text.toString(), digits) != minorUnits) {
            state.setTextAndPlaceCursorAtEnd(minorUnits?.let { DecimalInput.fromMinor(it, digits) }.orEmpty())
        }
    }
    LaunchedEffect(state, digits) {
        snapshotFlow { state.text.toString() }.collect { onChange(DecimalInput.toMinor(it, digits)) }
    }
    AppDecimalField(
        state = state,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helper = helper,
        error = error,
        enabled = enabled,
        maxDecimals = digits,
        maxIntegerDigits = MoneyIntegerDigits,
        prefix = currency.getSymbol(locale),
        imeAction = imeAction,
    )
}

/** The rules behind the decimal fields, apart from Compose so they can be tested. */
object DecimalInput {
    /**
     * [candidate] as a field should hold it, or null when the keystroke must be refused. The
     * locale's [decimalSeparator] (a `,` in German) is stored as `.`; anything else that is not a
     * digit is dropped, so a pasted "1,234.50" lands as "1234.50"; a second point is refused.
     */
    fun accept(candidate: String, maxDecimals: Int, maxIntegerDigits: Int, decimalSeparator: Char = DecimalPoint): String? {
        val cleaned = buildString {
            candidate.forEach { c ->
                when {
                    c.isDigit() -> append(c)
                    c == DecimalPoint || c == decimalSeparator -> append(DecimalPoint)
                }
            }
        }
        if (cleaned.count { it == DecimalPoint } > 1) return null
        if (maxDecimals == 0 && DecimalPoint in cleaned) return null
        val whole = cleaned.substringBefore(DecimalPoint)
        val fraction = cleaned.substringAfter(DecimalPoint, "")
        if (whole.length > maxIntegerDigits || fraction.length > maxDecimals) return null
        return cleaned
    }

    /** `"99.5"` with 2 digits is 9950; null for an empty or point-only field. */
    fun toMinor(text: String, digits: Int): Long? {
        if (text.isEmpty() || text == DecimalPoint.toString()) return null
        val whole = text.substringBefore(DecimalPoint).ifEmpty { "0" }.toLongOrNull() ?: return null
        val fraction = text.substringAfter(DecimalPoint, "").padEnd(digits, '0').take(digits).ifEmpty { "0" }.toLongOrNull() ?: return null
        return whole * pow10(digits) + fraction
    }

    /** 9950 with 2 digits is `"99.50"`, 10000 is `"100"`: the shortest text that says it exactly. */
    fun fromMinor(minor: Long, digits: Int): String {
        val unit = pow10(digits)
        val whole = minor / unit
        val rest = minor % unit
        return if (rest == 0L) whole.toString() else whole.toString() + DecimalPoint + rest.toString().padStart(digits, '0')
    }

    /** Where grouping separators go in a whole part of [length] digits: every three from the right. */
    fun groupingPositions(length: Int): List<Int> = (length - GroupSize downTo 1 step GroupSize).toList()

    private fun pow10(digits: Int): Long = (1..digits).fold(1L) { acc, _ -> acc * 10 }

    internal const val DecimalPoint = '.'
    private const val GroupSize = 3
}

private class DecimalInputTransformation(
    private val maxDecimals: Int,
    private val maxIntegerDigits: Int,
    private val decimalSeparator: Char,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val accepted = DecimalInput.accept(asCharSequence().toString(), maxDecimals, maxIntegerDigits, decimalSeparator)
        when {
            accepted == null -> revertAllChanges()
            accepted != asCharSequence().toString() -> replace(0, length, accepted)
        }
    }
}

/** Shows the locale's point and grouping; the stored text keeps `.` and no separators. */
private class LocaleDecimalOutput(locale: Locale, private val grouping: Boolean) : OutputTransformation {
    private val symbols = DecimalFormatSymbols.getInstance(locale)

    override fun TextFieldBuffer.transformOutput() {
        val text = asCharSequence().toString()
        val point = text.indexOf(DecimalInput.DecimalPoint)
        if (point >= 0 && symbols.decimalSeparator != DecimalInput.DecimalPoint) {
            replace(point, point + 1, symbols.decimalSeparator.toString())
        }
        if (!grouping) return
        val whole = if (point >= 0) point else text.length
        // Inserted from the right, so earlier positions stay valid.
        DecimalInput.groupingPositions(whole).forEach { insert(it, symbols.groupingSeparator.toString()) }
    }
}

private const val MoneyIntegerDigits = 12
