"use client";

import { Icon } from "./icons.tsx";
import { Avatar, Button, Chip, Column, Divider, Group, InputField, LargeTitle, Row, SectionHeader, Surface, Switch, Text, useTheme } from "./parts.tsx";
import type { PreviewTab } from "./tabs.ts";
import { dp } from "./theme.ts";

// Every screen here is one the template ships, with its copy, laid out with its tokens.

const POSTS = [
  { title: "Sunt aut facere repellat provident", body: "Quia et suscipit suscipit recusandae consequuntur expedita et cum reprehenderit…" },
  { title: "Qui est esse", body: "Est rerum tempore vitae sequi sint nihil reprehenderit dolor beatae ea dolores…" },
  { title: "Ea molestias quasi exercitationem", body: "Et iusto sed quo iure voluptatem occaecati omnis eligendi aut ad voluptatem…" },
  { title: "Eum et est occaecati", body: "Ullam et saepe reiciendis voluptatem adipisci sit amet autem assumenda…" },
  { title: "Nesciunt quas odio", body: "Repudiandae veniam quaerat sunt sed alias aut fugiat sit autem sed est…" },
];

export type ScreenOptions = { features: ReadonlySet<string>; haptics: boolean };

export function ScreenBody({ tab, options }: { tab: PreviewTab; options: ScreenOptions }) {
  switch (tab.screen) {
    case "signin":
      return <SignIn google={options.features.has("googlesignin")} />;
    case "home":
      return <Home />;
    case "feed":
      return <Feed />;
    case "search":
      return <Search />;
    case "profile":
      return <Profile media={options.features.has("media")} />;
    case "settings":
      return <Settings options={options} />;
    case "module":
      return <Module title={tab.label} />;
  }
}

function SignIn({ google }: { google: boolean }) {
  const t = useTheme();
  return (
    <div style={{ padding: `${dp(32)}px ${dp(16)}px 0`, display: "flex", flexDirection: "column", gap: dp(16) }}>
      <Text role="largeTitle">Welcome back</Text>
      <InputField label="Email" placeholder="you@example.com" />
      <InputField label="Password" value="••••••••" focused />
      <Button label="Sign in" full />
      {google && (
        <>
          <div style={{ display: "flex", alignItems: "center", gap: dp(12) }}>
            <span style={{ flex: 1, height: 1, background: t.colour("divider") }} />
            <Text role="caption1" colour="contentTertiary">
              or
            </Text>
            <span style={{ flex: 1, height: 1, background: t.colour("divider") }} />
          </div>
          <Button label="Continue with Google" variant="secondary" full />
        </>
      )}
      <div style={{ display: "flex", justifyContent: "space-between" }}>
        <Button label="Create account" variant="plain" small />
        <Button label="Forgot password" variant="plain" small />
      </div>
    </div>
  );
}

function Home() {
  return (
    <>
      <LargeTitle title="Samples" subtitle="Pulled from a live API" />
      <div style={{ padding: `${dp(4)}px ${dp(16)}px ${dp(12)}px` }}>
        <InputField placeholder="Search titles" icon="search" />
      </div>
      <Column>
        {POSTS.map((post) => (
          <Surface key={post.title}>
            <Text role="headline">{post.title}</Text>
            <Text role="footnote" colour="contentTertiary" style={{ marginTop: dp(4) }}>
              {post.body}
            </Text>
          </Surface>
        ))}
      </Column>
    </>
  );
}

function Feed() {
  return (
    <>
      <LargeTitle title="Feed" subtitle="Loads ten at a time as you scroll" />
      <Column>
        {POSTS.map((post, index) => (
          <Surface key={post.title}>
            <div style={{ display: "flex", alignItems: "center", gap: dp(8), marginBottom: dp(8) }}>
              <Avatar name={`Author ${index + 1}`} />
              <Text role="subheadline" colour="contentSecondary">{`Author ${index + 1}`}</Text>
            </div>
            <Text role="headline">{post.title}</Text>
            <Text role="callout" colour="contentSecondary" style={{ marginTop: dp(6) }}>
              {post.body}
            </Text>
          </Surface>
        ))}
      </Column>
    </>
  );
}

function Search() {
  return (
    <>
      <LargeTitle title="Search" />
      <div style={{ padding: `${dp(4)}px ${dp(16)}px ${dp(8)}px` }}>
        <InputField placeholder="Search posts" icon="search" />
      </div>
      <Column gap={8}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: dp(4) }}>
          <Text role="subheadline" colour="contentSecondary">
            Recent
          </Text>
          <Button label="Clear" variant="plain" small />
        </div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: dp(8) }}>
          {["weather", "sunt aut", "dolor", "qui est esse"].map((query) => (
            <Chip key={query} label={query} icon="clock" />
          ))}
        </div>
      </Column>
    </>
  );
}

function Profile({ media }: { media: boolean }) {
  return (
    <>
      <LargeTitle title="Profile" />
      <Column gap={14}>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: dp(8) }}>
          <Avatar name="Ada Lovelace" size={88} />
          <Text role="title3">Ada Lovelace</Text>
          {media && <Button label="Change photo" variant="secondary" small />}
        </div>
        <InputField label="Name" value="Ada Lovelace" />
        <InputField label="About you" placeholder="A line or two for your profile" tall />
        <Button label="Save changes" full />
      </Column>
    </>
  );
}

function Settings({ options }: { options: ScreenOptions }) {
  const t = useTheme();
  const { features, haptics } = options;
  const account = features.has("auth");
  const about = features.has("licenses") || features.has("browser");
  return (
    <>
      <LargeTitle title="Settings" />
      <Column gap={8}>
        <SectionHeader title="Appearance" />
        <Surface>
          <Text role="body">Theme</Text>
          <Text role="footnote" colour="contentTertiary" style={{ marginBottom: dp(10) }}>
            System follows your device setting.
          </Text>
          <div style={{ display: "flex", background: t.colour("fill"), borderRadius: dp(t.style.radii.sm), padding: dp(3) }}>
            {["System", "Light", "Dark"].map((option, index) => {
              const on = index === (t.dark ? 2 : 0);
              return (
                <span
                  key={option}
                  style={{
                    ...t.type("footnote"),
                    flex: 1,
                    textAlign: "center",
                    padding: `${dp(6)}px 0`,
                    borderRadius: dp(Math.max(0, t.style.radii.sm - 3)),
                    background: on ? t.colour("surface") : "transparent",
                    color: on ? t.colour("contentPrimary") : t.colour("contentTertiary"),
                  }}
                >
                  {option}
                </span>
              );
            })}
          </div>
        </Surface>
        <Group>
          <Row icon="bell" title="Haptic feedback" supporting="A small vibration when a control responds." trailing={<Switch on={haptics} />} />
          {features.has("language") && (
            <>
              <Divider />
              <Row icon="globe" title="Language" supporting="Same as your phone" />
            </>
          )}
          {features.has("applock") && (
            <>
              <Divider />
              <Row icon="lock" title="App lock" trailing={<Switch on={false} />} />
            </>
          )}
        </Group>
        {about && (
          <>
            <SectionHeader title="About" />
            <Group>
              {features.has("browser") && <Row icon="doc" title="Privacy policy" />}
              {features.has("browser") && features.has("licenses") && <Divider />}
              {features.has("licenses") && <Row icon="doc" title="Open-source licences" />}
            </Group>
          </>
        )}
        {account && (
          <>
            <SectionHeader title="Account" />
            <Group>
              <Row icon="logout" title="Sign out" danger />
            </Group>
          </>
        )}
      </Column>
    </>
  );
}

/** A scaffolded module's screen: its title and the empty state its strings ship with. */
function Module({ title }: { title: string }) {
  const t = useTheme();
  return (
    <>
      <LargeTitle title={title} />
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", padding: `${dp(96)}px ${dp(32)}px 0`, gap: dp(8) }}>
        <Icon name="inbox" size={dp(36)} colour={t.colour("contentTertiary")} />
        <Text role="title3">Nothing here yet</Text>
        <Text role="subheadline" colour="contentSecondary">
          When there is something to show, it appears on this screen.
        </Text>
      </div>
    </>
  );
}
