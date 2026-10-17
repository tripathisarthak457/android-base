---
name: claude-app-appdata-redirect
description: Claude desktop is an MSIX app; agent-shell writes to %APPDATA% are silently redirected into its package storage
metadata:
  node_type: memory
  type: project
  originSessionId: 82dbecb7-5b01-4185-ab6e-5ad90ad4fa80
  modified: 2026-10-17T06:32:47.586Z
---

The Claude desktop app is packaged (MSIX, package Claude_pzs8sxrjxfjjc). Files that the agent's own PowerShell/Bash tool writes under C:\Users\crazyhusir\AppData\Roaming are silently redirected to
%LOCALAPPDATA%\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Roaming\<name>. The agent's shell still sees them, but other programs and the user's own terminals don't. Program Files, HKCU\Environment and D:\ are not redirected.

Found on 2026-10-17: the first `npm install -g vercel` landed in the redirected storage, and an earlier session's `go telemetry off` was also redirected, so real Go telemetry was still "local".

**Why:** setup that writes to %APPDATA% from the agent shell looks fine to the agent but doesn't actually happen, and it leaves stray copies the user has to clean up.

**How to apply:** run any command that writes to %APPDATA% (npm -g, go telemetry, CLI logins/config) in a Terminal panel tab via run_in_terminal, which isn't redirected. Check results from that tab too, not from the agent shell.
