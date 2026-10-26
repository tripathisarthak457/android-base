---
name: wear-widgets
description: Create, migrate, update, and verify Wear OS widgets using Glance Wear
  and Remote Compose in an Android project. Use this skill when implementing GlanceWearWidget,
  GlanceWearWidgetService, WearWidgetDocument, Remote Compose UI primitives (RemoteColumn,
  RemoteRow, RemoteBox, RemoteText, RemoteImage, RemoteButton), local state with rememberMutableRemoteInt
  and valueChange, click actions with pendingIntentAction, widget provider XML containers
  (SMALL and LARGE), rectangular widget picker PNG previews, or migrating legacy ProtoLayout
  TileService tiles to Wear OS widgets. Don't use for mobile home screen AppWidgetProvider
  or GlanceAppWidget implementations, watch face complications, or full-screen Wear
  OS activities.
---

Google's `wear-widgets` skill is vendored at `.agents/skills/wear-widgets/` (Android skills v1.0.17, Apache-2.0).
First read the `wear-widgets` row of the "Google's Android skills" table in `CLAUDE.md`: it lists where this project
differs, and that table wins wherever the two disagree. Then read `.agents/skills/wear-widgets/SKILL.md` and resolve every
relative path in it from that folder.
