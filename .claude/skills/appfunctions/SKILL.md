---
name: appfunctions
description: Analyzes Android apps to identify key user workflows for AppFunctions
  such as creating a note, playing media, or sending an automated or AI agent triggered
  message, voice commands, or system shortcuts, without needing to open the app UI.
  Generates Kotlin code to expose these workflows to the Android system, allowing
  agents to discover and execute them on-device. Also refines KDoc documentation to
  ensure AI agents correctly understand and use the provided functionality.
---

Google's `appfunctions` skill (Android skills v1.0.17, Apache-2.0) is vendored for the generated projects at `templates/android/.agents/skills/appfunctions/`.
Read `templates/android/.agents/skills/appfunctions/SKILL.md` and resolve every relative path in it from that folder.
When you change a template, also read the `appfunctions` row of the "Google's Android skills" table in that template's `CLAUDE.md`:
it lists where the generated projects differ, and it wins wherever the two disagree.
