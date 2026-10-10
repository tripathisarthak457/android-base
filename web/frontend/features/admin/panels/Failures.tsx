"use client";

import { useState } from "react";
import { Button } from "../../../components/primitives.tsx";
import { Badge } from "../../../components/ui.tsx";
import { type ErrorGroup, type Errors as ErrorsData, type Failures as FailuresData, type Rate, adminPost, useAdminAction, useAdminQuery } from "../api.ts";
import { percent } from "../charts.tsx";
import { Panel, type PanelProps, Table } from "../Panel.tsx";

function rate({ n, failed }: Rate): string {
  return n ? `${percent(failed / n, 1)} of ${n}` : "—";
}

/** Rejected specs by reason, and failures of specs that reached the generator, by what they had in common. */
export function Failures({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<FailuresData>("failures", token, query, version, onUnauthorized);
  return (
    <Panel title="Failures" note="failed = generator error or timeout" result={result}>
      {(data) => {
        const byFeature = [...data.by_feature].filter((row) => row.on.failed > 0).sort((a, b) => b.on.failed / (b.on.n || 1) - a.on.failed / (a.on.n || 1));
        return (
          <div className="grid gap-6 lg:grid-cols-2">
            <div>
              <h3 className="mb-2 text-xs font-medium text-ink-300">Top rejection reasons</h3>
              <Table head={["Field", "Reason", "Count"]} empty={data.rejections.length === 0}>
                {data.rejections.map((row) => (
                  <tr key={`${row.field}-${row.reason}`} className="text-ink-200">
                    <td className="py-1.5 pr-3 font-mono">{row.field || "—"}</td>
                    <td className="py-1.5 pr-3 text-right">{row.reason}</td>
                    <td className="py-1.5 text-right font-mono">{row.n}</td>
                  </tr>
                ))}
              </Table>
            </div>
            <div>
              <h3 className="mb-2 text-xs font-medium text-ink-300">Failure rate with each feature on and off</h3>
              <Table head={["Feature", "On", "Off"]} empty={byFeature.length === 0}>
                {byFeature.map((row) => (
                  <tr key={row.key} className="text-ink-200">
                    <td className="py-1.5 pr-3 font-mono">{row.key}</td>
                    <td className="py-1.5 text-right font-mono">{rate(row.on)}</td>
                    <td className="py-1.5 text-right font-mono">{rate(row.off)}</td>
                  </tr>
                ))}
              </Table>
            </div>
            <div>
              <h3 className="mb-2 text-xs font-medium text-ink-300">Pairs that fail together (5 projects or more)</h3>
              <Table head={["Pair", "Failed"]} empty={data.pairs.length === 0}>
                {data.pairs.map((row) => (
                  <tr key={`${row.a}+${row.b}`} className="text-ink-200">
                    <td className="py-1.5 pr-3 font-mono">
                      {row.a} + {row.b}
                    </td>
                    <td className="py-1.5 text-right font-mono">{rate(row)}</td>
                  </tr>
                ))}
              </Table>
            </div>
            <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3">
              {(
                [
                  ["Min SDK", data.by_min_sdk],
                  ["Preset", data.by_preset],
                  ["Generator commit", data.by_commit],
                ] as const
              ).map(([label, rows]) => (
                <div key={label}>
                  <h3 className="mb-2 text-xs font-medium text-ink-300">{label}</h3>
                  <Table head={["", "Failed"]} empty={rows.length === 0}>
                    {rows.map((row) => (
                      <tr key={row.key} className="text-ink-200">
                        <td className="py-1.5 pr-2 font-mono">{row.key.slice(0, 10) || "—"}</td>
                        <td className="py-1.5 text-right font-mono">{rate(row)}</td>
                      </tr>
                    ))}
                  </Table>
                </div>
              ))}
            </div>
          </div>
        );
      }}
    </Panel>
  );
}

/** Errors grouped by fingerprint: one bug is one row however often it fired. Tracebacks expand. */
export function Errors({ token, query, version, onUnauthorized }: PanelProps) {
  const [resolved, setResolved] = useState(false);
  const [changed, setChanged] = useState<Record<number, boolean>>({});
  const save = useAdminAction(onUnauthorized);
  const result = useAdminQuery<ErrorsData>("errors", token, `${query}&resolved=${resolved}&limit=100`, version, onUnauthorized);
  // A reload is the server's word, which already holds this tab's changes and may hold another admin's.
  const [changedFor, setChangedFor] = useState(result.data);
  if (changedFor !== result.data) {
    setChangedFor(result.data);
    setChanged({});
  }

  async function toggle(group: ErrorGroup, next: boolean) {
    if (await save.run(() => adminPost("errors/resolve", token, { id: group.id, resolved: next }))) {
      setChanged((current) => ({ ...current, [group.id]: next }));
    }
  }

  return (
    <Panel
      title="Errors"
      note="last seen in the window"
      result={result}
      actions={
        <Button variant="ghost" size="small" onClick={() => setResolved((current) => !current)}>
          {resolved ? "Hide resolved" : "Show resolved"}
        </Button>
      }
    >
      {(data) => (
        <>
          {save.problem && <p className="mb-3 text-xs text-rose">{save.problem}</p>}
          {data.items.length === 0 ? (
            <p className="rounded-lg border border-mint/25 bg-mint/5 p-4 text-sm text-mint">Nothing broken.</p>
          ) : (
            <ul className="divide-y divide-ink-800">
              {data.items.map((group) => {
                const isResolved = changed[group.id] ?? group.resolved;
                return (
                  <li key={group.id} className="py-3">
                    <div className="flex items-start gap-3">
                      <Badge tone={isResolved ? "neutral" : "amber"}>{group.kind}</Badge>
                      <details className="min-w-0 flex-1">
                        <summary className="cursor-pointer list-none">
                          <span className={`block truncate text-sm ${isResolved ? "text-ink-400 line-through" : "text-ink-100"}`}>{group.message}</span>
                          <span className="mt-0.5 block font-mono text-[11px] text-ink-400">
                            ×{group.occurrences} · last {new Date(group.last_seen).toLocaleString()} · {group.path || "—"} ·{" "}
                            {group.fingerprint.slice(0, 8)}
                          </span>
                        </summary>
                        <pre className="mt-3 max-h-80 overflow-auto whitespace-pre-wrap rounded-lg bg-ink-900 p-3 font-mono text-[11px] leading-relaxed text-ink-300">
                          {group.detail || "No traceback was recorded."}
                        </pre>
                      </details>
                      <Button variant="ghost" size="small" onClick={() => void toggle(group, !isResolved)} disabled={save.busy}>
                        {isResolved ? "Reopen" : "Resolve"}
                      </Button>
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
