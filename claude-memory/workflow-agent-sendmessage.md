---
name: workflow-agent-sendmessage
description: Never SendMessage a running workflow agent (it resumes as a separate copy); resume caches match by call order
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-26T01:22:12.407Z
---

Never send SendMessage to an agent that a Workflow run spawned. On Oct 26 06:44 a message to the final run's `final:after` agent resumed it as a separate background agent, outside the workflow. Stopping that copy also ended the workflow's step, and the run had to be relaunched.

**Why:** two copies of one agent in the same worktree would collide, and the workflow loses track of its step.

**How to apply:**
- To change what a running workflow step does, edit the script, stop the run at a step boundary and relaunch it with `resumeFromRunId`.
- Resume caches match by the order of `agent()` calls (longest unchanged prefix). Keep the lane order identical, for example the same `args.first`.
- An aborted relaunch with a different order breaks the chain. Then pass finished steps explicitly in `args.done`; small placeholders are enough when nothing reads their content.

Related: [[workflow-model-routing]], [[usage-reserve-10-percent]].
