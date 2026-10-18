Overhaul autopilot check (every 20 minutes). The user said: keep running until I tell you to stop; do the best and achieve the goal; never compromise quality (Opus 5.5 on every agent (the user, Oct 15 20:55; no Fable); the Google/AOSP review before every merge, phone gates on release builds only). Android first (REQUIREMENTS 36).
1. Read D:/projects/tools/.overhaul/STATUS.md §1 for the running workflows (task ids, run ids, script paths).
2. If they are all still running: do nothing else (no log line; keep the check cheap).
3. If one ended:
   - Read its result and failures. Usage caps (the user, Oct 17): 90% of the 5-hour window during 09:00-21:00 IST (and before 09:00 for a window that resets after 09:00), up to 99% overnight (stop at ~97%). At the cap, stop the workflow, note it in STATUS §1 and PROGRESS, and schedule a one-shot resume 3 minutes after the reset.
   - If it ended because of a usage-limit cutoff, an API error or a restart, relaunch it with Workflow({scriptPath: <its script path in STATUS §1>, resumeFromRunId: <its last run id>}). Record the new task id and run id in STATUS.md §1.
   - If a package ended blocked or merge-failed, read its handoff and the journal, fix the cause (edit the script or the handoff), then resume as above.
4. If .overhaul/RELEASE-GATE-android.md exists and says GO: tell the user at once that the Android release is ready, with the gate's summary and report/FINAL.md's before/after table, and ask for their go-ahead to merge to main and push (Vercel deploys). Never push or merge to main without that explicit go-ahead in chat.
5. Append one line to PROGRESS.md when you relaunch, merge or finish.
6. After a merge into overhaul/multiplatform-v2, back up off-machine (the user's standing OK, 2026-10-17): scan what is new for secrets, then git -C D:/projects/tools/android-base push origin 'refs/heads/wp/*:refs/heads/wp/*' 'refs/heads/overhaul/*:refs/heads/overhaul/*' 'refs/tags/merged/*:refs/tags/merged/*'. Never main, never --force, never delete a remote branch; agents inside workflows never push.
Never push PeriFlow; never kill processes you did not start.
