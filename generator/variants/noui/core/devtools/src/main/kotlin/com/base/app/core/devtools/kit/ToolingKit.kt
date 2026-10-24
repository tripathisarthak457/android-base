package com.base.app.core.devtools.kit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.StatusColors
import kotlinx.coroutines.launch

/*
 * The developer tools' own controls, in a project without UI components: the panel, the badge and
 * the performance overlay keep the names they are written against, drawn plainly on Compose
 * foundation and the theme. They are the tools' chrome, not the app's design system, and they ship
 * only where the tools do: :core:devtools is left out of production builds, which each
 * verify<Flavour>ReleaseCarriesNoDevTooling task proves.
 */

/** Text in the theme's type and colours. */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = AppTheme.typography.body,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color.takeOrElse { style.color.takeOrElse { AppTheme.colors.contentPrimary } },
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
        overflow = overflow,
        maxLines = maxLines,
    )
}

/** Values the eye compares down a column: timings, sizes, keys. */
@Composable
fun AppMonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = AppTheme.typography.mono,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    AppText(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
    )
}

enum class ButtonVariant { Filled, Tinted, Plain, Destructive }

enum class ButtonSize { Small, Medium, Large }

/** A labelled button, at least the height its [size] names. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val (container, content) = when (variant) {
        ButtonVariant.Filled -> colors.accent to colors.onAccent
        ButtonVariant.Tinted -> colors.accentSubtle to colors.accentContent
        ButtonVariant.Plain -> Color.Transparent to colors.accentContent
        ButtonVariant.Destructive -> colors.danger.base to colors.danger.on
    }
    val height = when (size) {
        ButtonSize.Small -> AppTheme.sizes.buttonSmall
        ButtonSize.Medium -> AppTheme.sizes.buttonMedium
        ButtonSize.Large -> AppTheme.sizes.buttonLarge
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .clip(AppTheme.shapes.md)
            .background(container)
            .pressable(onClick = onClick, enabled = enabled, role = Role.Button)
            .padding(horizontal = AppTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = text,
            style = AppTheme.typography.subheadline,
            color = if (enabled) content else colors.contentDisabled,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A raised group of rows or text on the theme's surface. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.surface,
    border: BorderStroke? = BorderStroke(Dp.Hairline, AppTheme.colors.separator),
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = AppTheme.shapes.md
    Column(
        modifier = modifier
            .clip(shape)
            .background(color, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** A one-pixel separator, drawn as a filled rect so it never blurs across two pixel rows. */
@Composable
fun AppDivider(modifier: Modifier = Modifier, color: Color = AppTheme.colors.separator) {
    val pixel = with(LocalDensity.current) { 1f.toDp() }
    Canvas(modifier = modifier.fillMaxWidth().height(pixel)) { drawRect(color) }
}

/** An on-off switch. A null [onCheckedChange] draws it without input, for a row that toggles. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val input = if (onCheckedChange != null) {
        Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(input)
            .size(width = TrackWidth, height = TrackHeight)
            .clip(AppTheme.shapes.pill)
            .background(if (checked) colors.accent else colors.fill)
            .padding(ThumbInset),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(TrackHeight - ThumbInset * 2)
                .clip(AppTheme.shapes.pill)
                .background(colors.segmentThumb),
        )
    }
}

/** A row whose whole width toggles [checked]. */
@Composable
fun AppToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppText(text = title, modifier = Modifier.weight(1f))
        AppSwitch(checked = checked, onCheckedChange = null)
    }
}

/** A list row: a title, an optional line under it, and something at its end. */
@Composable
fun AppRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val press = if (onClick != null) {
        Modifier.pressable(onClick = onClick, role = Role.Button)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(press)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            AppText(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                AppText(
                    text = subtitle,
                    style = AppTheme.typography.footnote,
                    color = AppTheme.colors.contentSecondary,
                )
            }
        }
        trailing?.invoke()
    }
}

/** A short status in its colours: an HTTP code, a grade. */
@Composable
fun AppStatusPill(text: String, status: StatusColors, modifier: Modifier = Modifier) {
    AppText(
        text = text,
        modifier = modifier
            .background(status.subtle, AppTheme.shapes.pill)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xxs),
        style = AppTheme.typography.caption1,
        color = status.content,
        maxLines = 1,
    )
}

/** What an empty list says instead of being blank. */
@Composable
fun AppStateView(modifier: Modifier = Modifier, title: String? = null, message: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AppTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        if (title != null) {
            AppText(
                text = title,
                style = AppTheme.typography.headline,
                textAlign = TextAlign.Center,
            )
        }
        if (message != null) {
            AppText(
                text = message,
                style = AppTheme.typography.subheadline,
                color = AppTheme.colors.contentSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One choice of a few, all in view: the panel's tabs. */
@Composable
fun AppSegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.md)
            .background(colors.fill)
            .padding(AppTheme.spacing.xxs),
    ) {
        options.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppTheme.shapes.sm)
                    .background(if (index == selected) colors.segmentThumb else Color.Transparent)
                    .selectable(
                        selected = index == selected,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    )
                    .padding(vertical = AppTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = label,
                    style = AppTheme.typography.subheadline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

object AppScrollDefaults {
    /** The platform's fling: the tools are measured against it, not against the app's own. */
    @Composable
    fun flingBehavior(): FlingBehavior = ScrollableDefaults.flingBehavior()
}

/** How a pressed control answers the finger. */
enum class PressFeedback { None, Scale }

/** A click target that answers a press as [feedback] says, never with a ripple or a grey veil. */
fun Modifier.pressable(
    onClick: () -> Unit,
    feedback: PressFeedback = PressFeedback.Scale,
    enabled: Boolean = true,
    role: Role? = null,
): Modifier = clickable(
    interactionSource = null,
    indication = if (feedback == PressFeedback.Scale) PressScale else null,
    enabled = enabled,
    role = role,
    onClick = onClick,
)

/**
 * How far content the app draws over sits from the bottom of the window. A project without UI
 * components has no bottom bar, so it is always nothing.
 */
val LocalBottomChrome: ProvidableCompositionLocal<State<Dp>> =
    staticCompositionLocalOf { NoBottomChrome }

private val NoBottomChrome: State<Dp> = mutableStateOf(0.dp)

/**
 * Shrinks what it decorates a little while it is pressed: a draw-time scale, so nothing is laid
 * out again.
 */
private object PressScale : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressScaleNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}

private class PressScaleNode(private val source: InteractionSource) :
    Modifier.Node(),
    DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                pressed = when (interaction) {
                    is PressInteraction.Press -> true
                    is PressInteraction.Release, is PressInteraction.Cancel -> false
                    else -> pressed
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (pressed) scale(PRESSED_SCALE) { this@draw.drawContent() } else drawContent()
    }
}

private const val PRESSED_SCALE = 0.96f
private val TrackWidth = 44.dp
private val TrackHeight = 26.dp
private val ThumbInset = 2.dp
