#!/usr/bin/env python3
"""Extract every message the user typed from Claude Code session transcripts.

Written on 2026-10-16 for the android-base overhaul handover (before the laptop was
wiped for Windows). It reads the top-level session files (*.jsonl, not subagents) of
the given project folders and keeps only what a human typed:

  * kind "typed":  a `user` entry whose origin is human (or, in older transcripts
                   with no origin field, a plain prompt), content as a string or as
                   text blocks. Tool results, system reminders, command caveats,
                   task notifications, <agent-message> blocks, skill bodies and
                   compaction summaries are dropped.
  * kind "queued": a `queued_command` attachment with commandMode "prompt": a message
                   the user typed while Claude was busy. It is delivered to Claude
                   mid-turn, so it never appears as a `user` entry of its own.
  * kind "slash":  a slash command the user ran (/model, /compact, ...).
  * kind "bash":   a shell command the user ran from the prompt with "!".
  * kind "answer": the user's answers to Claude's AskUserQuestion prompts. They arrive
                   as tool results, but the user chose or typed them, so they are kept.
  * kind "interrupt": the "[Request interrupted by user]" marker (the user pressed Esc).

Auto-inserted context (<ide_opened_file>, <system-reminder>) is removed from the text
and noted. Attached images are counted, not copied. Strings shaped like secrets are
redacted (see SECRET_PATTERNS); the count is reported.

Compaction summaries ("This session is being continued from a previous conversation")
are not user messages. With --summaries FILE they are written to a separate Markdown
file for reading. They may quote local dev credentials, so keep that file private.

Usage:
  python3 extract_user_messages.py --json out.json [--md USER-MESSAGES.md]
                                   [--summaries summaries.md] [DIR ...]

With no DIR, the three folders of this project on the old Linux laptop are used.
Timestamps in the transcripts are UTC; the Markdown shows IST (UTC+5:30), the user's
time zone, with UTC alongside.
"""

import argparse
import datetime as dt
import glob
import json
import os
import re
import sys

DEFAULT_DIRS = [
    os.path.expanduser("~/.claude/projects/-home-crazyhusir-Documents-projects-tools"),
    os.path.expanduser("~/.claude/projects/-home-crazyhusir-Documents-projects-tools--overhaul"),
    os.path.expanduser("~/.claude/projects/-home-crazyhusir-Downloads-android-base"),
]

# Sessions that are about another project. They are listed in the output as skipped.
SKIP_SESSIONS = {
    "27c61945-53ac-463e-8eae-c7a58c055aa3": (
        "Sep 27, 2026: an unrelated project ('medmosaic', a separate web tool), started "
        "from the tools folder. Not about android-base, PeriFlow or the laptop."
    ),
}

IST = dt.timezone(dt.timedelta(hours=5, minutes=30))

SECRET_PATTERNS = [
    re.compile(r"gh[pousr]_[A-Za-z0-9]{20,}"),
    re.compile(r"github_pat_[A-Za-z0-9_]{20,}"),
    re.compile(r"sk-[A-Za-z0-9_-]{20,}"),
    re.compile(r"AIza[0-9A-Za-z_-]{30,}"),
    re.compile(r"AKIA[0-9A-Z]{16}"),
    re.compile(r"xox[abpr]-[A-Za-z0-9-]{10,}"),
    re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"),
    re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    re.compile(r"(?i)\b(password|passwd|storePassword|keyPassword|secret|api[_-]?key|token)\b\s*[:=]\s*\S+"),
    re.compile(r"postgres(ql)?://[^\s:]+:[^\s@]+@"),
]

DROP_PREFIXES = (
    "<task-notification",
    "<local-command-caveat",
    "<local-command-stdout",
    "<local-command-stderr",
    "<bash-stdout",
    "<bash-stderr",
    "<agent-message",
    "<command-message",
    "<system-reminder",
    "This session is being continued from a previous conversation",
    "Base directory for this skill:",
    "[Image: original",
    "Caveat: The messages below",
)

COMPACT_PREFIX = "This session is being continued from a previous conversation"


def redact(text):
    hits = 0
    for pat in SECRET_PATTERNS:
        text, n = pat.subn("<redacted>", text)
        hits += n
    return text, hits


def blocks_to_text(content):
    """Return (text, image_count, has_tool_result) for a string or a block list."""
    if isinstance(content, str):
        return content, 0, False
    texts, images, tool = [], 0, False
    for block in content or []:
        kind = block.get("type")
        if kind == "text":
            texts.append(block.get("text", ""))
        elif kind == "image":
            images += 1
        elif kind == "tool_result":
            tool = True
    return "\n".join(texts), images, tool


def clean(text):
    """Strip auto-inserted context; return (text, notes)."""
    notes = []
    for m in re.finditer(r"<ide_opened_file>(.*?)</ide_opened_file>", text, re.S):
        notes.append("IDE context: " + m.group(1).strip())
    text = re.sub(r"<ide_opened_file>.*?</ide_opened_file>", "", text, flags=re.S)
    for m in re.finditer(r"<ide_selection>(.*?)</ide_selection>", text, re.S):
        notes.append("IDE selection: " + m.group(1).strip()[:200])
    text = re.sub(r"<ide_selection>.*?</ide_selection>", "", text, flags=re.S)
    if "<system-reminder>" in text:
        notes.append("an auto-inserted <system-reminder> was removed")
    text = re.sub(r"<system-reminder>.*?</system-reminder>", "", text, flags=re.S)
    return text.strip(), notes


def parse_slash(text):
    name = re.search(r"<command-name>(.*?)</command-name>", text, re.S)
    if not name:
        return None
    args = re.search(r"<command-args>(.*?)</command-args>", text, re.S)
    out = name.group(1).strip()
    if args and args.group(1).strip():
        out += " " + args.group(1).strip()
    return out


def parse_bash(text):
    m = re.search(r"<bash-input>(.*?)</bash-input>", text, re.S)
    return ("!" + m.group(1).strip()) if m else None


def extract_file(path):
    session = os.path.basename(path)[: -len(".jsonl")]
    messages, summaries = [], []
    seen_uuid, seen_queued = set(), set()
    tool_names = {}  # tool_use id -> tool name, to find AskUserQuestion answers
    with open(path, encoding="utf-8", errors="replace") as fh:
        for line in fh:
            try:
                d = json.loads(line)
            except ValueError:
                continue
            kind = d.get("type")
            ts = d.get("timestamp")

            if kind == "attachment":
                att = d.get("attachment") or {}
                if att.get("type") != "queued_command" or att.get("commandMode") != "prompt":
                    continue
                key = att.get("source_uuid") or (ts, str(att.get("prompt"))[:80])
                if key in seen_queued:
                    continue
                seen_queued.add(key)
                text, images, _ = blocks_to_text(att.get("prompt"))
                text, notes = clean(text)
                if text.startswith(DROP_PREFIXES):
                    continue
                messages.append(dict(session=session, ts=att.get("timestamp") or ts,
                                     kind="queued", text=text, images=images, notes=notes,
                                     uuid=att.get("source_uuid")))
                continue

            if kind == "assistant":
                for block in (d.get("message") or {}).get("content") or []:
                    if isinstance(block, dict) and block.get("type") == "tool_use":
                        tool_names[block.get("id")] = block.get("name")
                continue
            if kind != "user":
                continue
            uuid = d.get("uuid")
            if uuid in seen_uuid:
                continue
            seen_uuid.add(uuid)
            content = (d.get("message") or {}).get("content")
            text, images, tool = blocks_to_text(content)
            if tool:
                for block in content:
                    if block.get("type") != "tool_result":
                        continue
                    if tool_names.get(block.get("tool_use_id")) != "AskUserQuestion":
                        continue
                    answer, _, _ = blocks_to_text(block.get("content"))
                    messages.append(dict(session=session, ts=ts, kind="answer", text=answer.strip(),
                                         images=0, notes=[], uuid=uuid))
                continue
            if text.startswith("[Request interrupted by user"):
                messages.append(dict(session=session, ts=ts, kind="interrupt", text=text.strip(),
                                     images=0, notes=[], uuid=uuid))
                continue
            if d.get("isCompactSummary") or text.startswith(COMPACT_PREFIX):
                summaries.append(dict(session=session, ts=ts, text=text))
                continue
            origin = d.get("origin")
            origin_kind = origin.get("kind") if isinstance(origin, dict) else origin

            slash = parse_slash(text) if "<command-name>" in text else None
            if slash and not d.get("isMeta"):
                messages.append(dict(session=session, ts=ts, kind="slash", text=slash,
                                     images=0, notes=[], uuid=uuid))
                continue
            bash = parse_bash(text) if "<bash-input>" in text else None
            if bash:
                messages.append(dict(session=session, ts=ts, kind="bash", text=bash,
                                     images=0, notes=[], uuid=uuid))
                continue

            if d.get("isMeta"):
                continue
            if origin_kind is not None and origin_kind != "human":
                continue  # task-notification, peer (another Claude session), system
            text, notes = clean(text)
            if not text and not images:
                continue
            if text.startswith(DROP_PREFIXES):
                continue
            messages.append(dict(session=session, ts=ts, kind="typed", text=text,
                                 images=images, notes=notes, uuid=uuid))
    return messages, summaries


def fmt_ts(ts):
    t = dt.datetime.fromisoformat(ts.replace("Z", "+00:00"))
    return t.astimezone(IST).strftime("%Y-%m-%d %H:%M:%S IST"), t.strftime("%H:%M:%S")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("dirs", nargs="*", default=DEFAULT_DIRS)
    ap.add_argument("--json", help="write all extracted messages as JSON")
    ap.add_argument("--md", help="write the messages as Markdown (body only; a header can be prepended)")
    ap.add_argument("--summaries", help="write the compaction summaries as Markdown (keep private)")
    args = ap.parse_args()

    files = []
    for folder in args.dirs:
        files += sorted(glob.glob(os.path.join(folder, "*.jsonl")))  # top level only

    kept, skipped, all_summaries, redactions = [], [], [], 0
    per_session = {}
    for path in files:
        session = os.path.basename(path)[:-6]
        msgs, sums = extract_file(path)
        if session in SKIP_SESSIONS:
            skipped.append(dict(session=session, file=path, reason=SKIP_SESSIONS[session], messages=len(msgs)))
            continue
        for m in msgs:
            m["text"], n = redact(m["text"])
            redactions += n
            m["redactions"] = n
        kept += msgs
        all_summaries += sums
        per_session[session] = dict(file=path, messages=len(msgs), summaries=len(sums))

    kept.sort(key=lambda m: m["ts"])
    all_summaries.sort(key=lambda s: s["ts"])

    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump(dict(files=files, sessions=per_session, skipped=skipped,
                           redactions=redactions, messages=kept,
                           summary_timestamps=[s["ts"] for s in all_summaries]),
                      fh, ensure_ascii=False, indent=1)

    if args.md:
        out = []
        day = None
        for i, m in enumerate(kept, 1):
            local, utc = fmt_ts(m["ts"])
            if local[:10] != day:
                day = local[:10]
                out.append("\n## " + dt.date.fromisoformat(day).strftime("%A, %B %d, %Y").replace(" 0", " ") + "\n")
            label = {"typed": "typed", "queued": "typed while Claude was busy (queued)",
                     "slash": "slash command", "bash": "shell command run with !",
                     "answer": "answer to Claude's question (AskUserQuestion)",
                     "interrupt": "the user interrupted Claude (Esc)"}[m["kind"]]
            out.append(f"### {i}. {local[11:16]} IST ({local[:10]} {local[11:]}, {utc} UTC) · session `{m['session'][:8]}` · {label}\n")
            extra = []
            if m["images"]:
                extra.append(f"{m['images']} image(s) attached (not copied)")
            extra += m["notes"]
            if m.get("redactions"):
                extra.append(f"{m['redactions']} secret-shaped string(s) redacted")
            if extra:
                out.append("_" + "; ".join(extra) + "_\n")
            body = m["text"] if m["text"] else "(no text)"
            fence = "````" if "```" in body else "```"
            out.append(f"{fence}text\n{body}\n{fence}\n")
        with open(args.md, "w", encoding="utf-8") as fh:
            fh.write("\n".join(out).lstrip("\n") + "\n")

    if args.summaries:
        with open(args.summaries, "w", encoding="utf-8") as fh:
            fh.write("# Compaction summaries (private: may quote local dev credentials)\n\n")
            for s in all_summaries:
                local, utc = fmt_ts(s["ts"])
                fh.write(f"## {local} ({utc} UTC), session {s['session']}\n\n{s['text']}\n\n")

    print(f"files: {len(files)}; kept messages: {len(kept)}; skipped sessions: {len(skipped)}; "
          f"compaction summaries: {len(all_summaries)}; redactions: {redactions}", file=sys.stderr)
    for s, v in per_session.items():
        print(f"  {s}: {v['messages']} messages, {v['summaries']} summaries", file=sys.stderr)


if __name__ == "__main__":
    main()
