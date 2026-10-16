#!/usr/bin/env bash
# Read-only look at the phone's network state, to explain the app's offline screen.
S=9a7c74f4
adb -s $S shell 'dumpsys trust | grep -i devicelocked; dumpsys connectivity | grep -iE "Active default network|NetworkAgentInfo.*(WIFI|CELLULAR)" | head -5; cmd wifi status 2>/dev/null | head -3; settings get global airplane_mode_on; ping -c 1 -W 3 8.8.8.8 | tail -2; ping -c 1 -W 3 jsonplaceholder.typicode.com | tail -2'
echo NET-DIAG DONE
