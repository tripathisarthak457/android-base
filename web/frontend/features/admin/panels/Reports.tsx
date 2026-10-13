"use client";

import { useId, useState } from "react";
import { Button } from "../../../components/primitives.tsx";
import { Badge } from "../../../components/ui.tsx";
import { type Report, type Reports as ReportsData, adminPost, useAdminAction, useAdminQuery } from "../api.ts";
import { Panel, type PanelProps } from "../Panel.tsx";

const STATUSES = ["new", "triaged", "fixed", "wontfix"] as const;
const KIND_TONE: Record<string, "neutral" | "accent" | "mint" | "amber"> = { bug: "amber", idea: "accent", praise: "mint", question: "neutral" };

/** Reports from people, filtered by status on the server so an old unread one is never hidden past a page. */
export function Reports({ token, query, version, onUnauthorized }: PanelProps) {
  const [status, setStatus] = useState<string>("new");
  const statusName = useId();
  const [edits, setEdits] = useState<Record<number, Partial<Report>>>({});
  const save = useAdminAction(onUnauthorized);
  const result = useAdminQuery<ReportsData>("feedback", token, `${query}&status=${status}&limit=100`, version, onUnauthorized);
  // A reload is the server's word, which already holds this tab's changes and may hold another admin's.
  const [editsFor, setEditsFor] = useState(result.data);
  if (editsFor !== result.data) {
    setEditsFor(result.data);
    setEdits({});
  }

  async function update(id: number, change: { status?: string; notes?: string }) {
    if (await save.run(() => adminPost("feedback/update", token, { id, ...change }))) {
      setEdits((current) => ({ ...current, [id]: { ...current[id], ...change } }));
    }
  }

  return (
    <Panel
      title="Reports"
      result={result}
      actions={
        <div role="radiogroup" aria-label="Status" className="flex flex-wrap gap-1">
          {[...STATUSES, "all"].map((option) => (
            <label
              key={option}
              className={`cursor-pointer rounded-md px-2.5 py-1 text-xs font-medium capitalize has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
                status === option ? "bg-ink-700 text-ink-100" : "text-ink-300 hover:bg-ink-800"
              }`}
            >
              <input type="radio" name={statusName} checked={status === option} onChange={() => setStatus(option)} className="sr-only" />
              {option}
            </label>
          ))}
        </div>
      }
    >
      {(data) => (
        <>
          <p className="mb-3 text-xs text-ink-400">
            {data.counts.new} new · {data.counts.blocking} blocking · {data.counts.last7Days} this week · {data.counts.total} all time
          </p>
          {save.problem && <p className="mb-3 text-xs text-rose">{save.problem}</p>}
          {data.items.length === 0 ? (
            <p className="rounded-lg border border-ink-700 bg-ink-900 p-4 text-sm text-ink-400">
              Nothing {status === "all" ? "in this window" : `marked ${status}`}.
            </p>
          ) : (
            <ul className="divide-y divide-ink-800">
              {data.items.map((original) => {
                const item = { ...original, ...edits[original.id] };
                return (
                  <li key={item.id} className="py-3">
                    <div className="flex items-start gap-3">
                      <Badge tone={KIND_TONE[item.kind] ?? "neutral"}>{item.kind}</Badge>
                      {item.severity === "blocks" && <Badge tone="rose">blocks</Badge>}
                      <details className="min-w-0 flex-1">
                        <summary className="cursor-pointer list-none">
                          <span className="block truncate text-sm font-medium text-ink-100">{item.title}</span>
                          <span className="mt-0.5 block font-mono text-[11px] text-ink-400">
                            #{item.id} · {item.target || "—"} · {item.area || "unspecified"} · {new Date(item.createdAt).toLocaleString()}
                            {item.contact ? " · has contact" : ""}
                          </span>
                        </summary>
                        <div className="mt-3 grid gap-5 rounded-lg bg-ink-900 p-4 text-xs lg:grid-cols-2">
                          <div className="space-y-3">
                            {(
                              [
                                ["What happened", item.body],
                                ["Steps", item.steps],
                                ["Expected", item.expected],
                                ["Actual", item.actual],
                                ["Contact", item.contact],
                              ] as const
                            )
                              .filter(([, value]) => value)
                              .map(([label, value]) => (
                                <div key={label}>
                                  <p className="text-ink-400">{label}</p>
                                  <p className="mt-0.5 whitespace-pre-wrap text-ink-200">{value}</p>
                                </div>
                              ))}
                          </div>
                          <dl className="grid content-start gap-1">
                            {(
                              [
                                ["App", item.appName],
                                ["Package", item.packageName],
                                ["Preset", item.preset],
                                ["Min SDK", item.minSdk ? String(item.minSdk) : ""],
                                ["Typeface", item.fontName],
                                ["Motion", item.motionStyle],
                                ["Accent", item.accentColour],
                                ["Features", item.features.join(", ")],
                                ["Browser", item.userAgent],
                                ["Page", item.pageUrl],
                                ["Build", item.appVersion],
                              ] as const
                            )
                              .filter(([, value]) => value)
                              .map(([label, value]) => (
                                <div key={label} className="flex justify-between gap-3">
                                  <dt className="shrink-0 text-ink-400">{label}</dt>
                                  <dd className="truncate font-mono text-ink-300" title={value}>
                                    {value}
                                  </dd>
                                </div>
                              ))}
                          </dl>
                        </div>
                        <NoteBox key={item.notes} current={item.notes} busy={save.busy} onSave={(notes) => void update(item.id, { notes })} />
                      </details>
                      <select
                        value={item.status}
                        onChange={(event) => void update(item.id, { status: event.target.value })}
                        disabled={save.busy}
                        aria-label={`Status of report ${item.id}`}
                        className="shrink-0 rounded-md border border-ink-500 bg-ink-900 px-2 py-1 text-xs text-ink-200"
                      >
                        {STATUSES.map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </>
      )}
    </Panel>
  );
}

/** Holds the draft of a note. Keyed on the stored note, so a save or a reload replaces the draft. */
function NoteBox({ current, busy, onSave }: { current: string; busy: boolean; onSave: (notes: string) => void }) {
  const [value, setValue] = useState(current);
  return (
    <div className="mt-3 flex gap-2">
      <input
        value={value}
        onChange={(event) => setValue(event.target.value)}
        aria-label="Triage note"
        placeholder="Triage note: what you found, what you did"
        className="h-9 flex-1 rounded-lg border border-ink-500 bg-ink-900 px-3 text-xs text-ink-100 placeholder:text-ink-500 focus:border-accent focus:outline-none"
      />
      <Button variant="secondary" size="small" onClick={() => onSave(value.trim())} disabled={busy || value.trim() === current}>
        {value.trim() === "" && current ? "Clear note" : "Save note"}
      </Button>
    </div>
  );
}
