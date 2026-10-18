---
name: workflow-model-routing
description: "Workflow model routing the user asked for — Sonnet for easy reading/scouting, Opus as super-mod/advisor and for all real development; reader agents must not write files"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 54d217bc-3bc1-4cde-a497-1e8ae558bef6
  modified: 2026-10-18T17:10:43.772Z
---

In multi-agent workflows, use Sonnet for easy work: reading source, scouting, inventories, doc lookups and mechanical
edits. Always use Opus for the supervising "super-mod" and the advisors, and for all real design, development, review
and adversarial verification. The main session stays on Opus.

**Why:** the user's words on 2026-10-18 (Rust Nuvio rewrite): "use proper models like sonnet for tasks which are easy
like reading and always have a super mod and advisor as opus and run important and actual development and major tasks
on opus only".

**How to apply:**
- Set `model: 'sonnet'` on reader stages and `model: 'opus'` on advisor, critic, developer and final stages.
- This does not override the android-base overhaul rule "Opus 5.5 only" in `D:\projects\tools\CLAUDE.md` for that
  project.
- Even read-only agent types (Explore) can write to `/tmp` (`%TEMP%`) through shell redirection: the Phase-0 auditors
  did. Reader prompts must say "stdout only, never redirect to files".
- Size workflows to the usage window. The 22-agent Phase-0 workflow cost about 31% of one 5-hour window. See
  [[usage-reserve-10-percent]] and [[rust-nuvio-rewrite-plan]].
