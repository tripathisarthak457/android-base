---
name: laptop-memory-freeze
description: Linux-era freezes (Oct 10) and power-offs (Oct 16) on this same laptop; on Windows the user considers them a Linux problem, so locked.py keeps only a memory wait and a stop near the thermal trip
metadata:
  type: project
---

Same hardware as the Linux laptop. History (Pop!_OS): on Oct 10 the machine froze when 9 agents started Gradle builds
(zram swap, no OOM daemon); on Oct 16 it powered off three times, most likely from CPU heat under Gradle (package
95-100 C, acpitz 97 C against a 99 C trip). It was then wiped for Windows.

On Windows (2026-10-17) the user said those crashes and the heat were Linux problems and Windows should be fine. So
`.overhaul/bin/locked.py` dropped the Linux heat throttling (2 workers, nice, the 80 C wait) and keeps: one Gradle build
at a time; a free-memory wait (3.5 GB cold, 1.5 GB with a daemon resident, at least 1 GB of commit left); 4 Gradle
workers (for memory, not heat). Its CPU-temperature wait is off unless `THERMWAIT_C` is set. First Windows readings:
ACPI zone TZ00 59-65 C idle, 96-97 C under parallel agent load before any build (its _HOT trip is 372 K, about 99 C).
Free RAM was about 4 GB of 16 GB (Chrome, Claude, Spotify, and an unusually large wlanext).
**How to apply:** don't re-argue the heat; log the first builds' temperatures (`locked thermlog`) and report the
numbers. Start one workflow at a time while free memory is low. After an unexplained shutdown, check the Event Viewer
(Kernel-Power 41). Related: [[android-base-generator]].
