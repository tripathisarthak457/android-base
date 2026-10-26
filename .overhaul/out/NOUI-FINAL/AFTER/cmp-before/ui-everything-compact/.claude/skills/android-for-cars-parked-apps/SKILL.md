---
name: android-for-cars-parked-apps
description: Adapt existing Android video streaming apps, games, and web browsers
  to run as parked apps on Android Auto and Android Automotive OS. Covers manifest
  configuration, Google Play hardware feature declarations, compatibility mode, back
  navigation affordances, orientation and display continuity, controllable system
  bar insets, irregular display cutouts, driver distraction lifecycle handling, MediaSession
  playback restrictions with CarUxRestrictionsManager, background audio while driving,
  and browser sensitive data protection. Use this skill when migrating, building,
  or auditing parked apps for cars to meet Car App Quality guidelines. Don't use for
  media audio apps, templated Car App Library navigation, point of interest, internet
  of things, weather, or communication apps designed for use while driving.
---

Google's `android-for-cars-parked-apps` skill is vendored at `.agents/skills/android-for-cars-parked-apps/` (Android skills v1.0.17, Apache-2.0).
First read the `android-for-cars-parked-apps` row of the "Google's Android skills" table in `CLAUDE.md`: it lists where this project
differs, and that table wins wherever the two disagree. Then read `.agents/skills/android-for-cars-parked-apps/SKILL.md` and resolve every
relative path in it from that folder.
