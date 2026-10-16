---
name: laptop-memory-freeze
description: The user's laptop (Mi NoteBook Ultra, i5-11300H, 15 GB) froze from memory pressure (Oct 10) and powered off three times on Oct 16, most likely from CPU heat under Gradle builds; it was wiped for Windows/WSL2. Keep bin/locked and bin/memwait; watch heat and memory
metadata:
  type: project
---

Same hardware after the wipe: Xiaomi Mi NoteBook Ultra, i5-11300H (4 cores / 8 threads), 15 GB RAM. From Oct 16 the plan is
Windows 11 26H2 with the workspace inside WSL2 Ubuntu (recommended `.wslconfig` `memory=10GB`; confirm the actual setup with the user on arrival; see
`.overhaul/handover/RESUME-ON-WINDOWS.md` §2 and §7).

**Oct 10, 15:23: memory freeze (Linux).** No panic, OOM or thermal entry; a silent gap about 3 minutes after 9 agents
started Gradle builds and tests (zram swap at swappiness 180, no earlyoom). Gradle daemon 4 GB + Kotlin daemon 2 GB +
Chrome + Claude + parallel heavy commands exceed 15 GB. Oct 16 10:32: a second freeze, about 1 minute after five
workflows were launched at once on top of stale daemons.

**Oct 16: three sudden power-offs.** The most likely cause is CPU heat under Gradle builds: the CPU package sat at
95–100 °C and `acpitz` at 97 °C (hot trip 99 °C), with PL1 at 200 W in the MSR. Windows' Intel DTT driver manages this
power and thermal policy; Linux did not. Thermal sensor SEN3 (thermal_zone3) also read erratically (60–125 °C), which
first looked like the cause; it is now considered a secondary, unreliable sensor. Evidence: `.overhaul/logs/thermal.log`
and `.overhaul/SESSION-2026-10-16.md`.

**How to apply (on any OS):**
- Gradle only through `.overhaul/bin/locked gradle` (one build machine-wide, 2 workers at `nice 10`, memwait first, 15 min
  daemon idle timeout); adb through `bin/locked adb` (uses `flock -o`, so an adb server forked inside the lock cannot
  hold it forever; when both are needed, adb first, then gradle); the emulator only inside `bin/locked emu`.
- Other heavy commands through `bin/memwait`. It waits for the CPU to cool below 80 °C (inside WSL2 there are no host
  thermal zones, so use `.overhaul/handover/tools/memwait-wsl` or let the temperature loop pass through) and for free
  memory (3.5 GB cold, 1.5 GB with a daemon resident).
- Never raise the Gradle heap; keep one Gradle daemon (identical JVM args); never start the emulator during builds.
- Launch workflows two at a time; after any resume from sleep, check `free -m` first.
- On Windows, watch the first builds' CPU temperature (HWiNFO64 on the host) and check that the Intel DTT driver is
  installed. Lid "Do nothing" and no sleep while plugged in, or the work stops.
- Never kill processes you did not start (the auto-mode classifier blocks it); ask the user.
- After an unexplained crash: on Linux `last -x`, `journalctl -b -1` and the tail of `thermal.log`; on Windows the
  Event Viewer (Kernel-Power 41) and `wsl --status`. Then relaunch from the handover notes.
