---
name: appfunctions
description: Analyzes Android apps to identify key user workflows for AppFunctions
  such as creating a note, playing media, or sending an automated or AI agent triggered
  message, voice commands, or system shortcuts, without needing to open the app UI.
  Generates Kotlin code to expose these workflows to the Android system, allowing
  agents to discover and execute them on-device. Also refines KDoc documentation to
  ensure AI agents correctly understand and use the provided functionality.
---

Google's `appfunctions` skill is vendored at `.agents/skills/appfunctions/` (Android skills v1.0.17, Apache-2.0).
First read the `appfunctions` row of the "Google's Android skills" table in `CLAUDE.md`: it lists where this project
differs, and that table wins wherever the two disagree. Then read `.agents/skills/appfunctions/SKILL.md` and resolve every
relative path in it from that folder.
