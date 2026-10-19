"""
The resolved tokens as Kotlin: `internal object DesignTokens` in `:core:designsystem`. Plain values
and small classes, every spring resolved per motion style, and each palette as the `AppColors` the
theme draws with, its contrast-derived roles already worked out.
"""

from __future__ import annotations

from typing import Any

from . import number

HEADER = (
    "// Generated from tokens/*.json by generator/genkit/tokens. "
    "Do not edit; run python3 -m genkit.tokens --write."
)

#: A generated project owns its copy: there is no tokens/*.json behind it to regenerate from.
PROJECT_HEADER = (
    "// The look this project was generated with: palette, type scale, springs and design styles.\n"
    "// Edit the values here to change them app-wide; AppTheme and every component read them."
)

#: Where the Android copy lives, under the module's source root and the package path.
RELATIVE_PATH = "core/designsystem/src/main/kotlin/{package_path}/core/designsystem/theme/tokens/DesignTokens.kt"


def emit(tokens: dict[str, Any], package: str, header: str = HEADER) -> str:
    theme = f"{package}.core.designsystem.theme"
    lines = [
        header,
        f"package {theme}.tokens",
        "",
        "import androidx.compose.animation.core.CubicBezierEasing",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.unit.Dp",
        "import androidx.compose.ui.unit.TextUnit",
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.unit.sp",
        *(
            f"import {theme}.{name}"
            for name in (
                "AppColors", "AppDesignStyle", "AppMotionStyle", "BarIndicator", "BarTreatment", "CardTreatment",
                "FieldTreatment", "SheetMaterial", "StatusColors", "SurfaceTone", "TypeVoice",
            )
        ),
        "",
        "internal object DesignTokens {",
    ]
    sections = [
        _dp_object("Spacing", tokens["spacing"]),
        _dp_object("Radii", tokens["radii"]),
        _dp_object("Sizes", tokens["sizes"]),
        _borders(tokens["borders"]),
        _dp_object("Elevation", tokens["elevation"]),
        _type(tokens["type"]),
        _motion(tokens["motion"]),
        _sheet(tokens["motion"]["sheet"]),
        _palette(tokens["palette"]),
        _styles(tokens["styles"]),
        _defaults(tokens["defaults"]),
    ]
    for section in sections:
        lines.append("")
        lines.extend(_indent(section))
    lines.append("}")
    return "\n".join(lines) + "\n"


def _indent(lines: list[str]) -> list[str]:
    return [f"    {line}" if line else "" for line in lines]


def _float(value: float) -> str:
    return f"{repr(round(float(value), 4))}f"


def _dp(value: float) -> str:
    text = number(value)
    return f"({text}).dp" if text.startswith("-") else f"{text}.dp"


def _sp(value: float) -> str:
    text = number(value)
    return f"({text}).sp" if text.startswith("-") else f"{text}.sp"


def _color(hex_argb: str) -> str:
    return f"Color(0x{hex_argb.lstrip('#')})"


def _string(text: str) -> str:
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def _const(name: str) -> str:
    return name[:1].lower() + name[1:]


def _dp_object(name: str, values: dict[str, float]) -> list[str]:
    return [f"object {name} {{", *(f"    val {key}: Dp = {_dp(value)}" for key, value in values.items()), "}"]


def _borders(borders: dict[str, float]) -> list[str]:
    return [
        "object Borders {",
        "    /** Physical pixels, not dp: a hairline is one device pixel at every density. */",
        f"    const val hairlinePx: Int = {int(borders['hairlinePx'])}",
        f"    val width: Dp = {_dp(borders['width'])}",
        f"    val widthStrong: Dp = {_dp(borders['widthStrong'])}",
        "}",
    ]


def _type(type_tokens: dict[str, Any]) -> list[str]:
    voices = type_tokens["voices"]
    lines = [
        "/** One role of the type scale. Each voice changes the weight only. */",
        "class TypeRole(",
        "    val size: TextUnit,",
        "    val lineHeight: TextUnit,",
        "    val tracking: TextUnit,",
        *(f"    val weight{voice}: Int," for voice in voices),
        ")",
        "",
        "object Type {",
    ]
    for name, role in type_tokens["roles"].items():
        weights = ", ".join(str(role["weight"][voice]) for voice in voices)
        lines.append(
            f"    val {name} = TypeRole({_sp(role['size'])}, {_sp(role['lineHeight'])}, "
            f"{_sp(role['tracking'])}, {weights})"
        )
    lines.append("}")
    return lines


def _motion(motion: dict[str, Any]) -> list[str]:
    spring_names = list(next(iter(motion["styles"].values()))["springs"])
    scroll, navigation, tab_bar, skeleton = motion["scroll"], motion["navigation"], motion["tabBar"], motion["skeleton"]
    lines = [
        "/** A spring in iOS's terms: seconds to settle, and how far it overshoots (negative: overdamped). */",
        "class Spring(val response: Float, val bounce: Float)",
        "",
        "class MotionStyle(",
        "    val summary: String,",
        *(f"    val {name}: Spring," for name in spring_names),
        ")",
        "",
        "object Motion {",
    ]
    for style, values in motion["styles"].items():
        lines.append(f"    val {_const(style)} = MotionStyle(")
        lines.append(f"        summary = {_string(values['description'])},")
        for name, spring in values["springs"].items():
            lines.append(f"        {name} = Spring({_float(spring['response'])}, {_float(spring['bounce'])}),")
        lines.append("    )")
    lines += [
        "",
        f"    const val rubberBand: Float = {_float(scroll['rubberBand'])}",
        f"    const val flingFriction: Float = {_float(scroll['flingFriction'])}",
        f"    const val pagingFriction: Float = {_float(scroll['pagingFriction'])}",
        f"    const val overscrollReleaseStiffness: Float = {_float(scroll['overscrollRelease']['stiffness'])}",
        f"    const val overscrollReleaseDampingRatio: Float = {_float(scroll['overscrollRelease']['dampingRatio'])}",
        f"    const val visibilityThreshold: Float = {_float(motion['visibilityThreshold']['float'])}",
        f"    const val offsetVisibilityThresholdPx: Int = {int(motion['visibilityThreshold']['offsetPx'])}",
        f"    const val fadeShortMs: Int = {int(motion['fades']['shortMs'])}",
        f"    const val fadeLongMs: Int = {int(motion['fades']['longMs'])}",
        f"    val fadeEnterEasing = CubicBezierEasing({', '.join(_float(c) for c in motion['fades']['enterEasing'])})",
        f"    val fadeExitEasing = CubicBezierEasing({', '.join(_float(c) for c in motion['fades']['exitEasing'])})",
        f"    const val staggerStepMs: Int = {int(motion['stagger']['stepMs'])}",
        f"    const val staggerMaxItems: Int = {int(motion['stagger']['maxItems'])}",
        f"    val staggerOffset: Dp = {_dp(motion['stagger']['offset'])}",
        f"    const val pressScale: Float = {_float(motion['press']['scale'])}",
        f"    const val pressScaleLarge: Float = {_float(motion['press']['scaleLarge'])}",
        f"    const val pressScaleIcon: Float = {_float(motion['press']['scaleIcon'])}",
        f"    const val pressDimAlpha: Float = {_float(motion['press']['dimAlpha'])}",
        f"    const val highlightFadeOutMs: Int = {int(motion['press']['highlightFadeOutMs'])}",
        f"    const val navigationParallax: Float = {_float(navigation['parallax'])}",
        f"    const val navigationScrimAlphaLight: Float = {_float(navigation['scrimAlpha']['light'])}",
        f"    const val navigationScrimAlphaDark: Float = {_float(navigation['scrimAlpha']['dark'])}",
        f"    val navigationEdgeShadow: Dp = {_dp(navigation['edgeShadow'])}",
        f"    const val navigationEdgeShadowAlpha: Float = {_float(navigation['edgeShadowAlpha'])}",
        f"    const val navigationBackGateRelease: Float = {_float(navigation['backGateRelease'])}",
        f"    val tabRevealShift: Dp = {_dp(tab_bar['revealShift'])}",
        f"    const val tabRevealVeilAlpha: Float = {_float(tab_bar['revealVeilAlpha'])}",
        f"    val tabIndicatorHeight: Dp = {_dp(tab_bar['indicatorHeight'])}",
        f"    const val tabIndicatorMaxStretch: Float = {_float(tab_bar['indicatorMaxStretch'])}",
        f"    const val tabIndicatorSquash: Float = {_float(tab_bar['indicatorSquash'])}",
        f"    const val tabIconBounceScale: Float = {_float(tab_bar['iconBounceScale'])}",
        f"    val tabMinimizeHysteresis: Dp = {_dp(tab_bar['minimizeHysteresis'])}",
        f"    val skeletonBandWidth: Dp = {_dp(skeleton['bandWidth'])}",
        f"    const val skeletonTiltDegrees: Float = {_float(skeleton['tiltDegrees'])}",
        f"    const val skeletonSweepMs: Int = {int(skeleton['sweepMs'])}",
        f"    const val skeletonPauseMs: Int = {int(skeleton['pauseMs'])}",
        f"    val skeletonEasing = CubicBezierEasing({', '.join(_float(c) for c in skeleton['easing'])})",
        f"    val skeletonReferenceTravel: Dp = {_dp(skeleton['referenceTravel'])}",
        f"    const val skeletonMaxSlowdown: Float = {_float(skeleton['maxSlowdown'])}",
        "}",
    ]
    return lines


#: Sheet tokens that are lengths (dp), or speeds in dp per second, rather than plain numbers.
_SHEET_DP = {
    "detent": ("largeTopGap", "mediumMinGap", "minHeight"),
    "release": ("detentFlick",),
    "grabber": ("width", "height", "top", "touchTarget", "touchBand", "contentTop"),
    "header": ("top", "minHeight", "scrollEdgeHeight", "scrollEdgeRamp"),
    "keyboard": ("dragHidesAfter", "formLiftGap"),
    "form": ("maxHeight",),
    "side": ("inset", "backShrink", "backGrow", "backY"),
    "cardStack": ("corner", "deviceCornerMin"),
    "stack": ("peek",),
    "back": ("shrinkX", "shrinkY", "persistentFollow"),
    "glass": ("blur",),
    "shadow": ("blur", "y"),
    "actionSheet": ("rowHeight", "cardWidth"),
}
_SHEET_INT = {"stack": ("maxCoveredDrawn",), "choice": ("searchFrom",)}


def _sheet(sheet: dict[str, Any]) -> list[str]:
    """One object per group of the sheet section, so a value reads as its JSON path: `Sheet.Release.detentFlick`."""
    lines = ["object Sheet {"]
    for index, (group, values) in enumerate(sheet.items()):
        if index:
            lines.append("")
        lines.append(f"    object {group[:1].upper()}{group[1:]} {{")
        for key, value in values.items():
            if isinstance(value, list):
                lines.append(f"        val {key} = CubicBezierEasing({', '.join(_float(c) for c in value)})")
            elif isinstance(value, dict):
                for theme, themed in value.items():
                    name = f"{key}{theme.capitalize()}"
                    if isinstance(themed, str):
                        opaque = themed if len(themed) == 9 else "#FF" + themed[1:]
                        lines.append(f"        val {name}: Color = {_color(opaque)}")
                    else:
                        lines.append(f"        const val {name}: Float = {_float(themed)}")
            elif key in _SHEET_DP.get(group, ()):
                lines.append(f"        val {key}: Dp = {_dp(value)}")
            elif key in _SHEET_INT.get(group, ()):
                lines.append(f"        const val {key}: Int = {int(value)}")
            else:
                lines.append(f"        const val {key}: Float = {_float(value)}")
        lines.append("    }")
    lines.append("}")
    return lines


def _palette(palette: dict[str, Any]) -> list[str]:
    sample = palette["tones"][next(iter(palette["tones"]))]["light"]
    plain = [name for name, value in sample.items() if not isinstance(value, dict)]
    statuses = [name for name, value in sample.items() if isinstance(value, dict)]

    lines = ["object Palette {"]
    for index, (tone, themes) in enumerate(palette["tones"].items()):
        if index:
            lines.append("")
        lines.append(f"    object {tone} {{")
        for theme, roles in themes.items():
            lines.append(f"        val {theme} = AppColors(")
            for name in plain:
                lines.append(f"            {name} = {_color(roles[name])},")
            for name in statuses:
                fields = ", ".join(f"{field} = {_color(value)}" for field, value in roles[name].items())
                lines.append(f"            {name} = StatusColors({fields}),")
            lines.append(f"            isLight = {'true' if theme == 'light' else 'false'},")
            lines.append("        )")
        lines.append("    }")
    lines.append("}")
    return lines


def _styles(styles: dict[str, Any]) -> list[str]:
    radii = list(next(iter(styles.values()))["radii"])
    lines = [
        "class Style(",
        *(f"    val radius{key.capitalize()}: Dp," for key in radii),
        "    /** Null: a pill, as round as the component is tall. */",
        "    val buttonRadius: Dp?,",
        "    val chipRadius: Dp?,",
        "    val card: CardTreatment,",
        "    val field: FieldTreatment,",
        "    val bar: BarTreatment,",
        "    val indicator: BarIndicator,",
        "    val borderWidth: Dp,",
        "    val borderWidthStrong: Dp,",
        "    val uppercaseLabels: Boolean,",
        "    val offsetShadow: Dp,",
        "    /** The gap a sheet floats at, from the sides and bottom, at its partial detents; 0 keeps it attached. */",
        "    val sheetInset: Dp,",
        "    val sheetMaterial: SheetMaterial,",
        "    val surfaces: SurfaceTone,",
        "    val voice: TypeVoice,",
        ")",
        "",
        "object Styles {",
    ]
    for name, style in styles.items():
        # A style that needs a feature is fenced with it, so a project without the feature never
        # names a treatment only that feature's code knows.
        fences = [f"opt:{key}" for key in style["requires"]]
        lines += [f"    // <{fence}>" for fence in fences]
        lines.append(f"    val {_const(name)} = Style(")
        lines += [f"        radius{key.capitalize()} = {_dp(style['radii'][key])}," for key in radii]
        for key in ("buttonRadius", "chipRadius"):
            lines.append(f"        {key} = {'null' if style[key] == 'pill' else _dp(style[key])},")
        lines += [
            f"        card = CardTreatment.{style['card']},",
            f"        field = FieldTreatment.{style['field']},",
            f"        bar = BarTreatment.{style['bar']},",
            f"        indicator = BarIndicator.{style['indicator']},",
            f"        borderWidth = {_dp(style['borderWidth'])},",
            f"        borderWidthStrong = {_dp(style['borderWidthStrong'])},",
            f"        uppercaseLabels = {'true' if style['uppercaseLabels'] else 'false'},",
            f"        offsetShadow = {_dp(style['offsetShadow'])},",
            f"        sheetInset = {_dp(style['sheetInset'])},",
            f"        sheetMaterial = SheetMaterial.{style['sheetMaterial']},",
            f"        surfaces = SurfaceTone.{style['surfaces']},",
            f"        voice = TypeVoice.{style['voice']},",
            "    )",
        ]
        lines += [f"    // </{fence}>" for fence in reversed(fences)]
    lines.append("}")
    return lines


def _defaults(defaults: dict[str, Any]) -> list[str]:
    return [
        "/** The project's own choices, which `AppTheme` falls back to. */",
        "object Defaults {",
        f"    val designStyle: AppDesignStyle = AppDesignStyle.{defaults['designStyle']}",
        f"    val motionStyle: AppMotionStyle = AppMotionStyle.{defaults['motionStyle']}",
        f"    const val hapticsEnabled: Boolean = {'true' if defaults['hapticsEnabled'] else 'false'}",
        f"    const val fontName: String = \"{defaults['fontName']}\"",
        f"    const val monoFontName: String = \"{defaults['monoFontName']}\"",
        "}",
    ]
