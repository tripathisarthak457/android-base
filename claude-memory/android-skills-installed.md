---
name: android-skills-installed
description: Google's Android skills plugin 1.0.15 on Windows - its clone is shallow and its working files are CRLF; vendor skills from git objects
metadata:
  type: reference
---

The plugin `android-skills@android-skills` 1.0.15 (commit 5bda39f) is installed (global CLAUDE.md §5.4). Its
marketplace clone `C:\Users\crazyhusir\.claude\plugins\marketplaces\android-skills` is shallow (5bda39f only, so
`git diff 42dc227 5bda39f` fails), and because Git for Windows has `core.autocrlf=true` system-wide, its working files
are CRLF while the repository stores LF (checked by bytes; Git Bash's `grep -c $'\r'` wrongly reports 0).
**How to apply:** read the relevant SKILL.md before Android work. Anything that vendors skills into generated projects
copies from git objects (`git -C <clone> show 5bda39f:<path>`), never the working files. Never change that clone. The
overhaul's digest is `.overhaul/research/ANDROID-SKILLS-DIGEST.md`. Related: [[android-base-generator]].
