"use client";

import { useState } from "react";
import { Button } from "../../../components/primitives.tsx";
import { Badge } from "../../../components/ui.tsx";
import { type Health as HealthData, type Report, type Reports as ReportsData, adminPost, useAdminQuery } from "../api.ts";
import { percent } from "../charts.tsx";
import { Panel, type PanelProps, Table } from "../Panel.tsx";

const STATUSES = ["new", "triaged", "fixed", "wontfix"] as const;
const KIND_TONE: Record<string, "neutral" | "accent" | "mint" | "amber"> = { bug: "amber", idea: "accent", praise: "mint", question: "neutral" };

/** Reports from people, filtered by status on the server so an old unread one is never hidden past a page. */
export function Reports({ token, query, version, onUnauthorized }: PanelProps) {
  const [status, setStatus] = useState<string>("new");
  const [edits, setEdits] = useState<Record<number, Partial<Report>>>({});
  const [problem, setProblem] = useState<string | null>(null);
  const result = useAdminQuery<ReportsData>("feedback", token, `${query}&status=${status}&limit=100`, version, onUnauthorized);

  async function update(id: number, change: { status?: string; notes?: string }) {
    setProblem(null);
    try {
      await adminPost("feedback/update", token, { id, ...change });
      setEdits((current) => ({ ...current, [id]: { ...current[id], ...change } }));
    } catch (error) {
      setProblem(error instanceof Error ? error.message : "That did not save.");
    }
  }

  return (
    <Panel
      title="Reports"
      result={result}
      actions={
        <div role="radiogroup" aria-label="Status" className="flex gap-1">
          {[...STATUSES, "all"].map((option) => (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={status === option}
              onClick={() => setStatus(option)}
              className={`rounded-md px-2.5 py-1 text-xs font-medium capitalize ${status === option ? "bg-ink-700 text-ink-100" : "text-ink-300 hover:bg-ink-800"}`}
            >
              {option}
            </button>
          ))}
        </div>
      }
    >
      {(data) => (
        <>
          <p className="mb-3 text-xs text-ink-400">
            {data.counts.new} new · {data.counts.blocking} blocking · {data.counts.last7Days} this week · {data.counts.total} all time
          </p>
          {problem && <p className="mb-3 text-xs text-rose">{problem}</p>}
          {data.items.length === 0 ? (
            <p className="rounded-lg border border-ink-700 bg-ink-900 p-4 text-sm text-ink-400">Nothing {status === "all" ? "in this window" : `marked ${status}`}.</p>
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
                        <NoteBox current={item.notes} onSave={(notes) => void update(item.id, { notes })} />
                      </details>
                      <select
                        value={item.status}
                        onChange={(event) => void update(item.id, { status: event.target.value })}
                        aria-label={`Status of report ${item.id}`}
                        className="shrink-0 rounded-md border border-ink-600 bg-ink-900 px-2 py-1 text-xs text-ink-200"
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

function NoteBox({ current, onSave }: { current: string; onSave: (notes: string) => void }) {
  const [value, setValue] = useState(current);
  return (
    <div className="mt-3 flex gap-2">
      <input
        value={value}
        onChange={(event) => setValue(event.target.value)}
        aria-label="Triage note"
        placeholder="Triage note: what you found, what you did"
        className="h-9 flex-1 rounded-lg border border-ink-600 bg-ink-900 px-3 text-xs text-ink-100 placeholder:text-ink-500 focus:border-accent focus:outline-none"
      />
      <Button variant="secondary" size="small" onClick={() => onSave(value.trim())} disabled={value.trim() === current}>
        {value.trim() === "" && current ? "Clear note" : "Save note"}
      </Button>
    </div>
  );
}

export function Health({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<HealthData>("health", token, query, version, onUnauthorized);
  return (
    <Panel title="Route health" note="error rate counts 5xx" result={result}>
      {(data) => (
        <>
          <p className="mb-3 font-mono text-[11px] text-ink-400">
            database {data.database} · up {Math.round(data.uptime_s / 60)} min · targets {data.targets.join(", ")}
            {data.telemetry && ` · telemetry written ${data.telemetry.written}, dropped ${data.telemetry.dropped}, failed ${data.telemetry.failed}`}
          </p>
          <Table head={["Route", "Requests", "5xx", "p50", "p95"]} empty={data.routes.length === 0}>
            {data.routes.map((row) => (
              <tr key={row.route} className="text-ink-200">
                <td className="py-1.5 pr-3 font-mono">{row.route}</td>
                <td className="py-1.5 text-right font-mono">{row.requests}</td>
                <td className={`py-1.5 text-right font-mono ${row.error_rate > 0 ? "text-rose" : ""}`}>{percent(row.error_rate, 1)}</td>
                <td className="py-1.5 text-right font-mono">{row.median_ms} ms</td>
                <td className="py-1.5 text-right font-mono">{row.p95_ms} ms</td>
              </tr>
            ))}
          </Table>
        </>
      )}
    </Panel>
  );
}
