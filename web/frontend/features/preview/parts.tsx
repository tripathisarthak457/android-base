"use client";

import { type CSSProperties, type ReactNode, createContext, useContext } from "react";
import { Icon, type IconName } from "./icons.tsx";
import { type PreviewTheme, dp, radius } from "./theme.ts";

/** The design-system pieces the preview screens are made of, one per template component. */

export const ThemeContext = createContext<PreviewTheme | null>(null);

export function useTheme(): PreviewTheme {
  const theme = useContext(ThemeContext);
  if (!theme) throw new Error("A preview part was drawn outside AppPreview.");
  return theme;
}

export function Text({ role, colour = "contentPrimary", style, children }: { role: string; colour?: string; style?: CSSProperties; children: ReactNode }) {
  const t = useTheme();
  return <div style={{ ...t.type(role), color: t.colour(colour), ...style }}>{children}</div>;
}

export function LargeTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div style={{ padding: `${dp(8)}px ${dp(16)}px ${dp(8)}px` }}>
      <Text role="largeTitle">{title}</Text>
      {subtitle && (
        <Text role="subheadline" colour="contentTertiary" style={{ marginTop: dp(2) }}>
          {subtitle}
        </Text>
      )}
    </div>
  );
}

export function Column({ children, gap = 12 }: { children: ReactNode; gap?: number }) {
  return <div style={{ display: "flex", flexDirection: "column", gap: dp(gap), padding: `0 ${dp(16)}px` }}>{children}</div>;
}

/** Playful's drawn edge: a content-coloured border and a hard shadow offset down and to the right. */
function offsetEdge(t: PreviewTheme): CSSProperties {
  const { borderWidth, offsetShadow } = t.style;
  const ink = t.colour("contentPrimary");
  return { border: `${borderWidth}px solid ${ink}`, boxShadow: `${dp(offsetShadow)}px ${dp(offsetShadow)}px 0 ${ink}` };
}

export function Surface({ children, style }: { children: ReactNode; style?: CSSProperties }) {
  const t = useTheme();
  const { card, radii, borderWidth } = t.style;
  const base: CSSProperties = { borderRadius: dp(radii.md), background: t.colour("surface"), padding: dp(16) };
  const treatment: CSSProperties =
    card === "Raised"
      ? t.dark
        ? { border: `1px solid ${t.colour("border")}` }
        : { boxShadow: "0 2px 6px rgba(11,15,26,0.08), 0 1px 2px rgba(11,15,26,0.06)" }
      : card === "Offset"
        ? offsetEdge(t)
        : { border: `${borderWidth}px solid ${t.colour("border")}` };
  return <div style={{ ...base, ...treatment, ...style }}>{children}</div>;
}

export function Button({
  label,
  variant = "primary",
  small,
  full,
}: {
  label: string;
  variant?: "primary" | "secondary" | "plain";
  small?: boolean;
  full?: boolean;
}) {
  const t = useTheme();
  const { offsetShadow, borderWidth, uppercaseLabels, buttonRadius } = t.style;
  const offset = offsetShadow > 0 && variant !== "plain";
  const look: Record<string, CSSProperties> = {
    primary: { background: t.colour("accent"), color: t.colour("onAccent") },
    secondary: { background: t.colour("surface"), color: t.colour("contentPrimary"), border: `${borderWidth}px solid ${t.colour("borderStrong")}` },
    plain: { background: "transparent", color: t.colour("accentContent") },
  };
  return (
    <div
      style={{
        ...t.type(small ? "subheadline" : "headline"),
        ...look[variant],
        ...(offset ? offsetEdge(t) : {}),
        textTransform: uppercaseLabels ? "uppercase" : undefined,
        letterSpacing: uppercaseLabels ? "0.08em" : undefined,
        borderRadius: radius(buttonRadius),
        height: dp(small ? 36 : 48),
        padding: `0 ${dp(small ? 12 : 18)}px`,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        width: full ? "100%" : undefined,
        whiteSpace: "nowrap",
      }}
    >
      {label}
    </div>
  );
}

export function InputField({
  label,
  placeholder,
  value,
  focused,
  icon,
  tall,
}: {
  label?: string;
  placeholder?: string;
  value?: string;
  focused?: boolean;
  icon?: IconName;
  tall?: boolean;
}) {
  const t = useTheme();
  const { field, borderWidth, borderWidthStrong, radii, uppercaseLabels } = t.style;
  const width = focused ? borderWidthStrong : borderWidth;
  const line = focused ? t.colour("accent") : field === "Underlined" ? t.colour("borderStrong") : t.colour("border");
  const box: CSSProperties =
    field === "Filled"
      ? { background: t.colour("fill"), border: `${width}px solid ${focused ? line : "transparent"}`, borderRadius: dp(radii.sm), padding: `0 ${dp(12)}px` }
      : field === "Underlined"
        ? { borderBottom: `${width}px solid ${line}`, padding: `0 ${dp(2)}px` }
        : { background: t.colour("surface"), border: `${width}px solid ${line}`, borderRadius: dp(radii.sm), padding: `0 ${dp(12)}px` };
  return (
    <div>
      {label && (
        <Text role="footnote" colour="contentSecondary" style={{ marginBottom: dp(6), textTransform: uppercaseLabels ? "uppercase" : undefined }}>
          {label}
        </Text>
      )}
      <div
        style={{
          ...box,
          minHeight: dp(tall ? 84 : 48),
          display: "flex",
          alignItems: tall ? "flex-start" : "center",
          paddingTop: tall ? dp(12) : undefined,
          gap: dp(8),
        }}
      >
        {icon && <Icon name={icon} size={dp(18)} colour={t.colour("contentTertiary")} />}
        <span style={{ ...t.type("body"), color: value ? t.colour("contentPrimary") : t.colour("contentTertiary") }}>{value ?? placeholder}</span>
      </div>
    </div>
  );
}

export function Chip({ label, icon }: { label: string; icon?: IconName }) {
  const t = useTheme();
  return (
    <span
      style={{
        ...t.type("footnote"),
        display: "inline-flex",
        alignItems: "center",
        gap: dp(6),
        height: dp(32),
        padding: `0 ${dp(12)}px`,
        borderRadius: radius(t.style.chipRadius),
        border: `${t.style.borderWidth}px solid ${t.colour("border")}`,
        background: t.colour("surface"),
        color: t.colour("contentSecondary"),
      }}
    >
      {icon && <Icon name={icon} size={dp(14)} colour={t.colour("contentTertiary")} />}
      {label}
    </span>
  );
}

export function Avatar({ name, size = 40 }: { name: string; size?: number }) {
  const t = useTheme();
  const initials = name
    .split(" ")
    .map((word) => word.charAt(0))
    .join("")
    .slice(0, 2)
    .toUpperCase();
  const fills = ["accentSubtle", "secondarySubtle", "tertiarySubtle"];
  const fill = fills[[...name].reduce((sum, c) => sum + c.charCodeAt(0), 0) % fills.length] ?? "accentSubtle";
  return (
    <span
      style={{
        ...t.type("footnote"),
        width: dp(size),
        height: dp(size),
        fontSize: dp(size * 0.36),
        borderRadius: 999,
        background: t.colour(fill),
        color: t.colour("contentPrimary"),
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        flexShrink: 0,
      }}
    >
      {initials}
    </span>
  );
}

export function Row({
  title,
  supporting,
  icon,
  trailing,
  danger,
}: {
  title: string;
  supporting?: string;
  icon: IconName;
  trailing?: ReactNode;
  danger?: boolean;
}) {
  const t = useTheme();
  const tint = danger ? t.status("danger").content : t.colour("contentPrimary");
  return (
    <div style={{ display: "flex", alignItems: "center", gap: dp(12), padding: `${dp(12)}px ${dp(16)}px` }}>
      <Icon name={icon} size={dp(20)} colour={danger ? tint : t.colour("contentTertiary")} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ ...t.type("body"), color: tint }}>{title}</div>
        {supporting && <div style={{ ...t.type("footnote"), color: t.colour("contentTertiary") }}>{supporting}</div>}
      </div>
      {trailing ?? (!danger && <Icon name="chevron" size={dp(16)} colour={t.colour("contentTertiary")} />)}
    </div>
  );
}

export function Divider() {
  const t = useTheme();
  return <div style={{ height: 1, background: t.colour("separator"), marginLeft: dp(48) }} />;
}

/** The grouped-list section the settings screens use: rows on one surface, separated by hairlines. */
export function Group({ children }: { children: ReactNode }) {
  return <Surface style={{ padding: 0, overflow: "hidden" }}>{children}</Surface>;
}

export function Switch({ on }: { on: boolean }) {
  const t = useTheme();
  return (
    <span
      style={{
        width: dp(44),
        height: dp(26),
        borderRadius: 999,
        background: on ? t.colour("accent") : t.colour("borderStrong"),
        position: "relative",
        flexShrink: 0,
      }}
    >
      <span
        style={{ position: "absolute", top: dp(3), left: on ? dp(21) : dp(3), width: dp(20), height: dp(20), borderRadius: 999, background: "#ffffff" }}
      />
    </span>
  );
}

export function SectionHeader({ title }: { title: string }) {
  const t = useTheme();
  return (
    <div
      style={{
        ...t.type("footnote"),
        color: t.colour("contentTertiary"),
        textTransform: "uppercase",
        letterSpacing: "0.04em",
        padding: `${dp(10)}px ${dp(4)}px ${dp(2)}px`,
      }}
    >
      {title}
    </div>
  );
}
