---
name: python-alias-needs-localappdata
description: "On this machine a bare `python` run with a stripped environment (no LOCALAPPDATA) makes the Python install manager download and register a new Python in the working directory"
metadata:
  node_type: memory
  type: project
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-24T19:12:33.026Z
---

On 2026-10-24 23:27 an agent ran the android-base API's generator integration test with `PYTHON_BIN=python`. The API
starts child processes with a minimal environment, without `LOCALAPPDATA`. The Python Install Manager's `python` alias
then could not find the real install, downloaded Python 3.15.1 into the current directory, and re-pointed the user's
`HKCU\Software\Python\PythonCore\3.15`, the Apps entry `pymanager-pythoncore-3.15-64` and the Start menu shortcuts at
that folder: an unasked system change.

**Why:** the user's machine must never change without their explicit yes (global CLAUDE.md section 2), and this path
changes the registry silently from what looks like a test run.

**How to apply:** any process that runs Python from a stripped or custom environment (subprocess env, API child env,
CI-like sandboxes) must either keep `LOCALAPPDATA` or call the real interpreter by full path
(`C:\Users\crazyhusir\AppData\Local\Python\pythoncore-3.15-64\python.exe`). The repair is `py install --refresh` from a
Terminal tab, with the user's yes. Related: [[claude-app-appdata-redirect]], [[android-base-generator]].
