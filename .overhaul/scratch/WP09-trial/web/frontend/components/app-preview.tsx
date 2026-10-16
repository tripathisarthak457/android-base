"use client";

import { AnimatePresence, motion } from "framer-motion";
import type { CSSProperties, ReactNode } from "react";
import {
  DESIGN_TOKENS,
  type DesignStyleKey,
  type DesignTokens,
  MOTION_TOKENS,
  type MotionStyleKey,
  type Palette,
  composeSpring,
  palette as buildPalette,
} from "../lib/app-theme";

/*
 * The generated app, drawn in the browser. Every screen here is one the template ships, with its
 * real copy, laid out with the same tokens the Kotlin uses.
 */

export type Screen = "signin" | "home" | "feed" | "search" | "profile" | "settings";

export type PreviewConfig = {
  designStyle: DesignStyleKey;
  motionStyle: MotionStyleKey;
  accent: string;
  secondary?: string;
  tertiary?: string;
  fontName: string;
  dark: boolean;
  features: Set<string>;
};

type Ctx = { p: Palette; t: DesignTokens; ty: TypeScale; font: string; features: Set<string> };

/** Scale from dp to preview pixels: a 360dp phone drawn 280px wide. */
const DP = 0.78;
const dp = (value: number) => value * DP;

export type Tab = { screen: Screen; label: string; icon: IconName };

export function tabsFor(features: Set<string>): Tab[] {
  const tabs: Tab[] = [];
  if (features.has("sample")) tabs.push({ screen: "home", label: "Home", icon: "home" });
  if (features.has("paging")) tabs.push({ screen: "feed", label: "Feed", icon: "list" });
  if (features.has("search")) tabs.push({ screen: "search", label: "Search", icon: "search" });
  if (features.has("profile")) tabs.push({ screen: "profile", label: "Profile", icon: "user" });
  if (features.has("settings")) tabs.push({ screen: "settings", label: "Settings", icon: "settings" });
  return tabs;
}

/** Every screen this configuration has, in the order the app reaches them. */
export function screensFor(features: Set<string>): Screen[] {
  const screens: Screen[] = [];
  if (features.has("auth")) screens.push("signin");
  return [...screens, ...tabsFor(features).map((tab) => tab.screen)];
}

export function PhoneFrame({
  children,
  dark,
  width = 280,
}: {
  children: ReactNode;
  dark: boolean;
  width?: number;
}) {
  return (
    <div
      className="relative shrink-0 overflow-hidden rounded-[34px] border-[5px] shadow-[0_24px_60px_-20px_rgba(40,30,15,0.45)] ring-1 ring-ink-600"
      style={{
        width,
        height: width * 2.05,
        borderColor: dark ? "#2b2926" : "#1f1d1a",
        background: dark ? "#0b0e14" : "#f7f8fa",
      }}
    >
      {children}
    </div>
  );
}

export function AppPreview({
  config,
  screen,
  onScreen,
  width = 280,
}: {
  config: PreviewConfig;
  screen: Screen;
  onScreen?: (screen: Screen) => void;
  width?: number;
}) {
  const t = DESIGN_TOKENS[config.designStyle];
  const p = buildPalette(
    { accent: config.accent, secondary: config.secondary, tertiary: config.tertiary },
    config.dark,
    t.surfaces,
  );
  const ctx: Ctx = { p, t, ty: typeFor(t.voice), font: config.fontName, features: config.features };
  const tabs = tabsFor(config.features);
  const motionTokens = MOTION_TOKENS[config.motionStyle];
  const showBar = screen !== "signin" && tabs.length > 0;
  const selected = Math.max(0, tabs.findIndex((tab) => tab.screen === screen));

  return (
    <PhoneFrame dark={config.dark} width={width}>
      <div
        className="absolute inset-0 flex flex-col"
        style={{
          background: p.background,
          color: p.contentPrimary,
          fontFamily: `"${config.fontName}", ui-sans-serif, system-ui, sans-serif`,
        }}
      >
        <StatusBar p={p} />
        <div className="relative min-h-0 flex-1 overflow-hidden">
          {/* The same fade-through the shell uses between tabs: out quickly, in with a settle. */}
          <AnimatePresence initial={false} mode="popLayout">
            <motion.div
              key={`${screen}-${config.designStyle}`}
              className="absolute inset-0 overflow-hidden"
              initial={{ opacity: 0, scale: 0.985 }}
              animate={{
                opacity: 1,
                scale: 1,
                transition: {
                  duration: motionTokens.medium / 1000,
                  delay: motionTokens.instant / 1000,
                  ease: [0.05, 0.7, 0.1, 1],
                },
              }}
              exit={{
                opacity: 0,
                transition: { duration: (motionTokens.instant * 1.5) / 1000, ease: [0.3, 0, 0.8, 0.15] },
              }}
            >
              <ScreenBody ctx={ctx} screen={screen} />
            </motion.div>
          </AnimatePresence>
        </div>
        {showBar && (
          <BottomBar
            ctx={ctx}
            tabs={tabs}
            selected={selected}
            onSelect={onScreen ? (index) => onScreen(tabs[index].screen) : undefined}
            spring={composeSpring(motionTokens.sheetDamping, 460)}
          />
        )}
      </div>
    </PhoneFrame>
  );
}

function StatusBar({ p }: { p: Palette }) {
  return (
    <div
      className="flex shrink-0 items-center justify-between px-5 pt-2.5 pb-1 font-mono text-[10px]"
      style={{ color: p.contentSecondary }}
    >
      <span>9:41</span>
      <span className="flex items-center gap-1">
        <span className="inline-block h-[7px] w-[11px] rounded-[2px]" style={{ background: p.contentSecondary }} />
      </span>
    </div>
  );
}

function ScreenBody({ ctx, screen }: { ctx: Ctx; screen: Screen }) {
  switch (screen) {
    case "signin":
      return <SignInScreen ctx={ctx} />;
    case "home":
      return <HomeScreen ctx={ctx} />;
    case "feed":
      return <FeedScreen ctx={ctx} />;
    case "search":
      return <SearchScreen ctx={ctx} />;
    case "profile":
      return <ProfileScreen ctx={ctx} />;
    case "settings":
      return <SettingsScreen ctx={ctx} />;
  }
}

// ── Pieces, one per design-system component ─────────────────────────────────────

const baseType = {
  display: { fontSize: dp(24), lineHeight: `${dp(30)}px`, fontWeight: 700, letterSpacing: "-0.02em" },
  heading: { fontSize: dp(17), lineHeight: `${dp(23)}px`, fontWeight: 700 },
  title: { fontSize: dp(16), lineHeight: `${dp(22)}px`, fontWeight: 600 },
  titleSmall: { fontSize: dp(13), lineHeight: `${dp(18)}px`, fontWeight: 600 },
  body: { fontSize: dp(14), lineHeight: `${dp(21)}px`, fontWeight: 400 },
  bodySmall: { fontSize: dp(13), lineHeight: `${dp(19)}px`, fontWeight: 400 },
  caption: { fontSize: dp(11), lineHeight: `${dp(15)}px`, fontWeight: 400 },
  button: { fontSize: dp(15), lineHeight: `${dp(20)}px`, fontWeight: 700 },
  label: { fontSize: dp(11), lineHeight: `${dp(14)}px`, fontWeight: 600, letterSpacing: "0.03em" },
} satisfies Record<string, CSSProperties>;

type TypeScale = Record<keyof typeof baseType, CSSProperties>;

/** `withVoice` in StyleTones.kt: the same sizes, with the weights and tracking each voice moves. */
function typeFor(voice: DesignTokens["voice"]): TypeScale {
  switch (voice) {
    case "neutral":
      return baseType;
    case "bold":
      return {
        ...baseType,
        display: { ...baseType.display, fontWeight: 800 },
        heading: { ...baseType.heading, fontWeight: 800 },
        title: { ...baseType.title, fontWeight: 700 },
        button: { ...baseType.button, fontWeight: 800 },
      };
    case "editorial":
      return {
        ...baseType,
        display: { fontSize: dp(28), lineHeight: `${dp(34)}px`, fontWeight: 700, letterSpacing: "-0.03em" },
        heading: { ...baseType.heading, fontSize: dp(18), lineHeight: `${dp(24)}px`, letterSpacing: "-0.015em" },
        label: { ...baseType.label, letterSpacing: "0.14em" },
      };
    case "rounded":
      return {
        ...baseType,
        display: { ...baseType.display, fontWeight: 900 },
        heading: { ...baseType.heading, fontWeight: 800 },
        title: { ...baseType.title, fontWeight: 800 },
        titleSmall: { ...baseType.titleSmall, fontWeight: 700 },
        body: { ...baseType.body, lineHeight: `${dp(23)}px` },
        button: { ...baseType.button, fontWeight: 800 },
      };
  }
}

function LargeTitle({ ctx, title, subtitle }: { ctx: Ctx; title: string; subtitle?: string }) {
  return (
    <div style={{ padding: `${dp(12)}px ${dp(16)}px ${dp(8)}px` }}>
      <div style={{ ...ctx.ty.display, color: ctx.p.contentPrimary }}>{title}</div>
      {subtitle && (
        <div style={{ ...ctx.ty.bodySmall, color: ctx.p.contentTertiary, marginTop: dp(2) }}>
          {subtitle}
        </div>
      )}
    </div>
  );
}

function Card({ ctx, children, style }: { ctx: Ctx; children: ReactNode; style?: CSSProperties }) {
  const { p, t } = ctx;
  const base: CSSProperties = {
    borderRadius: dp(t.radius.md),
    background: p.surface,
    padding: dp(16),
  };
  const treatment: CSSProperties =
    t.card === "outlined"
      ? { border: `${t.border}px solid ${p.border}` }
      : t.card === "raised"
        ? p.dark
          ? { border: `1px solid ${p.border}` }
          : { boxShadow: "0 2px 6px rgba(11,15,26,0.08), 0 1px 2px rgba(11,15,26,0.06)" }
        : {
            border: `${t.border}px solid ${p.contentPrimary}`,
            boxShadow: `${dp(t.offsetShadow)}px ${dp(t.offsetShadow)}px 0 ${p.contentPrimary}`,
          };
  return <div style={{ ...base, ...treatment, ...style }}>{children}</div>;
}

function Button({
  ctx,
  label,
  variant = "primary",
  size = "medium",
  full,
}: {
  ctx: Ctx;
  label: string;
  variant?: "primary" | "secondary" | "ghost" | "tertiary";
  size?: "small" | "medium";
  full?: boolean;
}) {
  const { p, t } = ctx;
  const offset = t.offsetShadow > 0 && (variant === "primary" || variant === "secondary");
  const palette: Record<string, CSSProperties> = {
    primary: { background: p.accent.base, color: p.accent.on },
    secondary: { background: p.surface, color: p.contentPrimary, border: `${t.border}px solid ${p.borderStrong}` },
    ghost: { background: "transparent", color: p.contentSecondary },
    tertiary: { background: "transparent", color: p.accent.base },
  };
  const text = t.uppercase ? label.toUpperCase() : label;
  return (
    <div
      style={{
        ...(size === "small" ? ctx.ty.titleSmall : ctx.ty.button),
        ...palette[variant],
        ...(offset
          ? {
              border: `${t.border}px solid ${p.contentPrimary}`,
              boxShadow: `${dp(t.offsetShadow)}px ${dp(t.offsetShadow)}px 0 ${p.contentPrimary}`,
            }
          : {}),
        letterSpacing: t.uppercase ? "0.08em" : undefined,
        borderRadius: t.button >= 999 ? 999 : dp(t.button),
        height: dp(size === "small" ? 36 : 44),
        padding: `0 ${dp(size === "small" ? 12 : 18)}px`,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        width: full ? "100%" : undefined,
        whiteSpace: "nowrap",
      }}
    >
      {text}
    </div>
  );
}

function Field({
  ctx,
  label,
  placeholder,
  value,
  focused,
  icon,
  tall,
}: {
  ctx: Ctx;
  label?: string;
  placeholder?: string;
  value?: string;
  focused?: boolean;
  icon?: IconName;
  tall?: boolean;
}) {
  const { p, t } = ctx;
  const width = focused ? t.borderStrong : t.border;
  const colour = focused ? p.accent.base : t.field === "underlined" ? p.borderStrong : p.border;
  const box: CSSProperties =
    t.field === "outlined"
      ? { background: p.surface, border: `${width}px solid ${colour}`, borderRadius: dp(t.radius.sm), padding: `0 ${dp(12)}px` }
      : t.field === "filled"
        ? {
            background: p.surfaceVariant,
            border: `${width}px solid ${focused ? colour : "transparent"}`,
            borderRadius: dp(t.radius.sm),
            padding: `0 ${dp(12)}px`,
          }
        : { borderBottom: `${width}px solid ${colour}`, padding: `0 ${dp(2)}px` };
  return (
    <div>
      {label && (
        <div style={{ ...ctx.ty.titleSmall, color: p.contentSecondary, marginBottom: dp(8) }}>{label}</div>
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
        {icon && <Icon name={icon} size={dp(18)} colour={p.contentTertiary} />}
        <span style={{ ...ctx.ty.body, color: value ? p.contentPrimary : p.contentTertiary }}>
          {value ?? placeholder}
        </span>
      </div>
    </div>
  );
}

function Chip({ ctx, label, icon }: { ctx: Ctx; label: string; icon?: IconName }) {
  const { p, t } = ctx;
  return (
    <span
      style={{
        ...ctx.ty.titleSmall,
        display: "inline-flex",
        alignItems: "center",
        gap: dp(6),
        height: dp(34),
        padding: `0 ${dp(12)}px`,
        borderRadius: t.chip >= 999 ? 999 : dp(t.chip),
        border: `${t.border}px solid ${p.border}`,
        background: p.surface,
        color: p.contentSecondary,
      }}
    >
      {icon && <Icon name={icon} size={dp(14)} colour={p.contentTertiary} />}
      {label}
    </span>
  );
}

function Avatar({ ctx, name, size = 40 }: { ctx: Ctx; name: string; size?: number }) {
  const index = [...name].reduce((sum, c) => sum + c.charCodeAt(0), 0) % ctx.p.avatars.length;
  const initials = name
    .split(" ")
    .map((word) => word[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
  return (
    <span
      style={{
        ...ctx.ty.titleSmall,
        width: dp(size),
        height: dp(size),
        fontSize: dp(size * 0.34),
        borderRadius: 999,
        background: ctx.p.avatars[index],
        color: ctx.p.contentPrimary,
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

function ListRow({
  ctx,
  title,
  supporting,
  icon,
  trailing,
  danger,
}: {
  ctx: Ctx;
  title: string;
  supporting?: string;
  icon: IconName;
  trailing?: ReactNode;
  danger?: boolean;
}) {
  const { p } = ctx;
  return (
    <div style={{ display: "flex", alignItems: "center", gap: dp(12), padding: `${dp(12)}px ${dp(16)}px` }}>
      <Icon name={icon} size={dp(20)} colour={danger ? p.danger : p.contentTertiary} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ ...ctx.ty.title, fontSize: dp(15), color: danger ? p.danger : p.contentPrimary }}>{title}</div>
        {supporting && <div style={{ ...ctx.ty.bodySmall, color: p.contentTertiary }}>{supporting}</div>}
      </div>
      {trailing}
    </div>
  );
}

function Switch({ ctx, on }: { ctx: Ctx; on: boolean }) {
  const { p } = ctx;
  return (
    <span
      style={{
        width: dp(44),
        height: dp(26),
        borderRadius: 999,
        background: on ? p.accent.base : p.borderStrong,
        position: "relative",
        flexShrink: 0,
      }}
    >
      <span
        style={{
          position: "absolute",
          top: dp(3),
          left: on ? dp(21) : dp(3),
          width: dp(20),
          height: dp(20),
          borderRadius: 999,
          background: "#ffffff",
        }}
      />
    </span>
  );
}

function SectionHeader({ ctx, title }: { ctx: Ctx; title: string }) {
  return (
    <div
      style={{
        ...ctx.ty.label,
        color: ctx.p.contentTertiary,
        textTransform: "uppercase",
        padding: `${dp(10)}px ${dp(2)}px ${dp(2)}px`,
      }}
    >
      {title}
    </div>
  );
}

// ── Screens ─────────────────────────────────────────────────────────────────────

const POSTS = [
  { title: "Sunt aut facere repellat provident", body: "Quia et suscipit suscipit recusandae consequuntur expedita et cum reprehenderit…" },
  { title: "Qui est esse", body: "Est rerum tempore vitae sequi sint nihil reprehenderit dolor beatae ea dolores…" },
  { title: "Ea molestias quasi exercitationem", body: "Et iusto sed quo iure voluptatem occaecati omnis eligendi aut ad voluptatem…" },
  { title: "Eum et est occaecati", body: "Ullam et saepe reiciendis voluptatem adipisci sit amet autem assumenda…" },
];

function Column({ children, gap = 12, padding = 16 }: { children: ReactNode; gap?: number; padding?: number }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: dp(gap), padding: `0 ${dp(padding)}px` }}>
      {children}
    </div>
  );
}

function SignInScreen({ ctx }: { ctx: Ctx }) {
  const google = ctx.features.has("googlesignin");
  return (
    <div style={{ padding: `${dp(40)}px ${dp(16)}px 0`, display: "flex", flexDirection: "column", gap: dp(16) }}>
      <div style={{ ...ctx.ty.display, color: ctx.p.contentPrimary }}>Welcome back</div>
      <Field ctx={ctx} label="Email" placeholder="you@example.com" />
      <Field ctx={ctx} label="Password" value="••••••••" focused />
      <Button ctx={ctx} label="Sign in" full />
      {google && (
        <>
          <div style={{ display: "flex", alignItems: "center", gap: dp(12) }}>
            <span style={{ flex: 1, height: 1, background: ctx.p.divider }} />
            <span style={{ ...ctx.ty.caption, color: ctx.p.contentTertiary }}>or</span>
            <span style={{ flex: 1, height: 1, background: ctx.p.divider }} />
          </div>
          <Button ctx={ctx} label="Continue with Google" variant="secondary" full />
        </>
      )}
      <div style={{ display: "flex", justifyContent: "space-between" }}>
        <Button ctx={ctx} label="Create account" variant="ghost" size="small" />
        <Button ctx={ctx} label="Forgot password" variant="ghost" size="small" />
      </div>
    </div>
  );
}

function HomeScreen({ ctx }: { ctx: Ctx }) {
  return (
    <>
      <LargeTitle ctx={ctx} title="Samples" subtitle="Pulled from a live API" />
      <div style={{ padding: `${dp(4)}px ${dp(16)}px ${dp(12)}px` }}>
        <Field ctx={ctx} placeholder="Search titles" icon="search" />
      </div>
      <Column>
        {POSTS.map((post) => (
          <Card key={post.title} ctx={ctx}>
            <div style={{ ...ctx.ty.title, color: ctx.p.contentPrimary }}>{post.title}</div>
            <div style={{ ...ctx.ty.bodySmall, color: ctx.p.contentTertiary, marginTop: dp(4) }}>{post.body}</div>
          </Card>
        ))}
      </Column>
    </>
  );
}

function FeedScreen({ ctx }: { ctx: Ctx }) {
  return (
    <>
      <LargeTitle ctx={ctx} title="Feed" subtitle="Loads ten at a time as you scroll" />
      <Column>
        {POSTS.slice(0, 3).map((post, index) => (
          <Card key={post.title} ctx={ctx}>
            <div style={{ display: "flex", alignItems: "center", gap: dp(8), marginBottom: dp(8) }}>
              <Avatar ctx={ctx} name={`Author ${index + 1}`} />
              <span style={{ ...ctx.ty.titleSmall, color: ctx.p.contentSecondary }}>Author {index + 1}</span>
            </div>
            <div style={{ ...ctx.ty.title, color: ctx.p.contentPrimary }}>{post.title}</div>
            <div style={{ ...ctx.ty.body, color: ctx.p.contentSecondary, marginTop: dp(6) }}>{post.body}</div>
            <div style={{ marginTop: dp(4), marginLeft: -dp(12) }}>
              <Button ctx={ctx} label="Show more" variant="tertiary" size="small" />
            </div>
          </Card>
        ))}
      </Column>
    </>
  );
}

function SearchScreen({ ctx }: { ctx: Ctx }) {
  return (
    <>
      <LargeTitle ctx={ctx} title="Search" />
      <div style={{ padding: `${dp(4)}px ${dp(16)}px ${dp(8)}px` }}>
        <Field ctx={ctx} placeholder="Search posts" icon="search" />
      </div>
      <Column gap={8}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: dp(4) }}>
          <span style={{ ...ctx.ty.titleSmall, color: ctx.p.contentSecondary }}>Recent</span>
          <Button ctx={ctx} label="Clear" variant="ghost" size="small" />
        </div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: dp(8) }}>
          {["weather", "sunt aut", "dolor", "qui est esse"].map((query) => (
            <Chip key={query} ctx={ctx} label={query} icon="clock" />
          ))}
        </div>
      </Column>
    </>
  );
}

function ProfileScreen({ ctx }: { ctx: Ctx }) {
  return (
    <>
      <LargeTitle ctx={ctx} title="Profile" />
      <Column gap={14}>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: dp(8) }}>
          <Avatar ctx={ctx} name="Ada Lovelace" size={88} />
          <div style={{ ...ctx.ty.heading, fontSize: dp(15), color: ctx.p.contentPrimary }}>Ada Lovelace</div>
          {ctx.features.has("media") && (
            <Button ctx={ctx} label="Change photo" variant="secondary" size="small" />
          )}
        </div>
        <Field ctx={ctx} label="Name" value="Ada Lovelace" />
        <Field ctx={ctx} label="About you" placeholder="A line or two for your profile" tall />
        <Button ctx={ctx} label="Save changes" full />
      </Column>
    </>
  );
}

function SettingsScreen({ ctx }: { ctx: Ctx }) {
  const { p, t } = ctx;
  return (
    <>
      <LargeTitle ctx={ctx} title="Settings" />
      <Column gap={10}>
        <SectionHeader ctx={ctx} title="Appearance" />
        <Card ctx={ctx}>
          <div style={{ ...ctx.ty.title, fontSize: dp(15), color: p.contentPrimary }}>Theme</div>
          <div style={{ ...ctx.ty.caption, color: p.contentTertiary, marginBottom: dp(10) }}>
            System follows your device setting.
          </div>
          <div
            style={{
              display: "flex",
              background: p.surfaceVariant,
              borderRadius: dp(t.radius.sm),
              padding: dp(3),
            }}
          >
            {["System", "Light", "Dark"].map((option, index) => (
              <span
                key={option}
                style={{
                  ...ctx.ty.titleSmall,
                  flex: 1,
                  textAlign: "center",
                  padding: `${dp(6)}px 0`,
                  borderRadius: dp(Math.max(0, t.radius.sm - 3)),
                  background: index === (p.dark ? 2 : 0) ? p.surface : "transparent",
                  color: index === (p.dark ? 2 : 0) ? p.contentPrimary : p.contentTertiary,
                }}
              >
                {option}
              </span>
            ))}
          </div>
        </Card>
        <Card ctx={ctx} style={{ padding: 0 }}>
          <ListRow
            ctx={ctx}
            icon="bell"
            title="Haptic feedback"
            supporting="A small vibration when a control responds."
            trailing={<Switch ctx={ctx} on />}
          />
        </Card>
        {ctx.features.has("language") && (
          <Card ctx={ctx} style={{ padding: 0 }}>
            <ListRow ctx={ctx} icon="globe" title="Language" supporting="Same as your phone" />
          </Card>
        )}
        <SectionHeader ctx={ctx} title="Account" />
        <Card ctx={ctx} style={{ padding: 0 }}>
          <ListRow ctx={ctx} icon="logout" title="Sign out" danger />
        </Card>
      </Column>
    </>
  );
}

// ── The tab bar · AppBottomBar.kt ───────────────────────────────────────────────

function BottomBar({
  ctx,
  tabs,
  selected,
  onSelect,
  spring,
}: {
  ctx: Ctx;
  tabs: Tab[];
  selected: number;
  /** Absent for a thumbnail, which may sit inside a button of its own. */
  onSelect?: (index: number) => void;
  spring: ReturnType<typeof composeSpring>;
}) {
  const { p, t } = ctx;
  const floating = t.bar === "floating";
  const item = (tab: Tab, index: number) => {
    const on = index === selected;
    const chunky = t.bar === "chunky";
    const tint = chunky && on ? p.accent.on : on ? (t.bar === "minimal" ? p.contentPrimary : p.accent.base) : p.contentTertiary;
    const labelTint = on ? (chunky || t.bar === "minimal" ? p.contentPrimary : p.accent.base) : p.contentTertiary;
    const Item = onSelect ? "button" : "div";
    return (
      <Item
        key={tab.screen}
        {...(onSelect ? { type: "button" as const, onClick: () => onSelect(index), "aria-label": tab.label } : {})}
        className="relative z-10 flex min-w-0 flex-1 flex-col items-center justify-center"
        style={{ gap: dp(3), height: "100%" }}
      >
        <span
          style={{
            display: "inline-flex",
            padding: chunky ? `${dp(3)}px ${dp(14)}px` : 0,
            borderRadius: 999,
            background: chunky && on ? p.accent.base : "transparent",
            transition: "background-color 150ms",
          }}
        >
          <Icon name={tab.icon} size={dp(22)} colour={tint} />
        </span>
        <span
          style={{
            ...ctx.ty.label,
            fontSize: dp(10.5),
            color: labelTint,
            textTransform: t.uppercase ? "uppercase" : undefined,
            letterSpacing: t.uppercase ? "0.1em" : undefined,
            transition: "color 250ms",
            // One line with an ellipsis, as the Compose label is: a long uppercase label must not
            // push into its neighbour.
            display: "block",
            maxWidth: "100%",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {tab.label}
        </span>
      </Item>
    );
  };

  if (floating) {
    return (
      <div style={{ padding: `${dp(12)}px ${dp(28)}px ${dp(16)}px`, flexShrink: 0 }}>
        <div
          className="relative flex"
          style={{
            height: dp(64),
            borderRadius: 999,
            background: p.surface,
            padding: dp(6),
            boxShadow: p.dark ? undefined : "0 8px 20px rgba(11,15,26,0.14)",
            border: p.dark ? `1px solid ${p.border}` : undefined,
          }}
        >
          <motion.span
            className="absolute"
            style={{
              top: dp(6),
              bottom: dp(6),
              width: `calc((100% - ${dp(12)}px) / ${tabs.length})`,
              borderRadius: 999,
              background: p.accent.subtle,
            }}
            animate={{ left: `calc(${dp(6)}px + (100% - ${dp(12)}px) / ${tabs.length} * ${selected})` }}
            transition={spring}
          />
          {tabs.map(item)}
        </div>
      </div>
    );
  }

  return (
    <div
      className="relative flex shrink-0"
      style={{
        height: dp(64),
        background: p.surface,
        borderTop: t.bar === "chunky" ? `${t.border}px solid ${p.contentPrimary}` : `1px solid ${p.divider}`,
      }}
    >
      {t.bar === "minimal" && (
        <motion.span
          className="absolute top-0"
          style={{ width: dp(20), height: 2, background: p.contentPrimary }}
          animate={{ left: `calc((100% / ${tabs.length}) * ${selected} + (100% / ${tabs.length}) / 2 - ${dp(10)}px)` }}
          transition={spring}
        />
      )}
      {tabs.map(item)}
    </div>
  );
}

// ── Icons, drawn like AppIcons: 24-unit grid, 2px round strokes ─────────────────

export type IconName = "home" | "list" | "search" | "user" | "settings" | "clock" | "bell" | "globe" | "logout";

const PATHS: Record<IconName, string> = {
  home: "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z",
  list: "M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01",
  search: "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4.2-4.2",
  user: "M20 21a8 8 0 0 0-16 0M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9z",
  settings:
    "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
  clock: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2",
  bell: "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0",
  globe: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18",
  logout: "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9",
};

export function Icon({ name, size, colour }: { name: IconName; size: number; colour: string }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke={colour}
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      style={{ transition: "stroke 250ms", flexShrink: 0 }}
    >
      <path d={PATHS[name]} />
    </svg>
  );
}
