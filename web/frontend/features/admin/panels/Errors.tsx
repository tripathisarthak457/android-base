"use client";

import { useState } from "react";
import { Button } from "../../../components/primitives.tsx";
import { Badge } from "../../../components/ui.tsx";
import { type ErrorGroup, type Errors as ErrorsData, LIST_LIMIT, adminPost, useAdminAction, useAdminQuery } from "../api.ts";
import { Panel, type PanelProps } from "../Panel.tsx";

/** Errors grouped by fingerprint: one bug is one row however often it fired. Tracebacks expand. */
export function Errors({ token, query, version, onUnauthorized }: PanelProps) {
  const [resolved, setResolved] = useState(false);
  const [changed, setChanged] = useState<Record<number, boolean>>({});
  const save = useAdminAction(onUnauthorized);
  const result = useAdminQuery<ErrorsData>("errors", token, `${query}&resolved=${resolved}&limit=${LIST_LIMIT}`, version, onUnauthorized);
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
            <p className="rounded-lg border border-mint/25 bg-mint/5 p-4 text-sm text-mint">
              {resolved ? "No errors in this window, resolved or not." : "Nothing broken."}
            </p>
          ) : (
            <ul className="divide-y divide-ink-800">
              {data.items.map((group) => {
                const isResolved = changed[group.id] ?? group.resolved;
                return (
                  <li key={group.id} className="py-3">
                    <div className="flex items-start gap-3">
                      <Badge tone={isResolved ? "neutral" : "amber"}>{group.kind}</Badge>
                      <details className="min-w-0 flex-1">
                        <summary className="cursor-pointer list-none [&::-webkit-details-marker]:hidden">
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
                      <Button variant="ghost" size="small" onClick={() => void toggle(group, !isResolved)} disabled={save.busy} focusableWhenDisabled>
                        {isResolved ? "Reopen" : "Resolve"}
                      </Button>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
          {data.items.length >= LIST_LIMIT && (
            <p className="mt-3 text-xs text-ink-400">
              The first {LIST_LIMIT} are listed, {resolved ? "open ones before resolved ones, " : ""}by when each last fired.
              Choose an earlier window for older ones.
            </p>
          )}
        </>
      )}
    </Panel>
  );
}
