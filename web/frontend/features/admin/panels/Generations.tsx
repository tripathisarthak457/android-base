"use client";

import { Fragment, useState } from "react";
import { Button } from "../../../components/primitives.tsx";
import { type Generation, type Generations as GenerationsData, downloadCsv, useAdminAction, useAdminQuery } from "../api.ts";
import { OUTCOME_COLOUR, seconds } from "../charts.tsx";
import { Panel, type PanelProps } from "../Panel.tsx";

const PAGE = 50;
const CSV_ROWS = 10_000;

/** Every generation in the window, newest first, a page at a time; a row opens its sanitised spec. */
export function Generations({ token, query, version, onUnauthorized }: PanelProps) {
  // Pages are kept per filter, so a new filter starts again from the newest row on its first fetch.
  const [paging, setPaging] = useState<{ query: string; cursors: number[] }>({ query, cursors: [] });
  const cursors = paging.query === query ? paging.cursors : [];
  const [open, setOpen] = useState<number | null>(null);
  const csv = useAdminAction(onUnauthorized);
  const before = cursors.at(-1);
  const result = useAdminQuery<GenerationsData>("generations", token, `${query}&limit=${PAGE}${before ? `&before=${before}` : ""}`, version, onUnauthorized);

  return (
    <Panel
      title="Generations"
      note={`page ${cursors.length + 1}`}
      result={result}
      actions={
        <Button variant="secondary" size="small" onClick={() => void csv.run(() => downloadCsv(token, `${query}&limit=${CSV_ROWS}`))} disabled={csv.busy} focusableWhenDisabled>
          {csv.busy ? "Exporting…" : `CSV (up to ${CSV_ROWS.toLocaleString()})`}
        </Button>
      }
    >
      {(data) => (
        <>
          {csv.problem && <p className="mb-3 text-xs text-rose">{csv.problem}</p>}
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-xs">
              <thead>
                <tr className="text-ink-400">
                  {["When", "Outcome", "App", "Target", "Preset", "Features", "Style", "SDK", "Time", "Where"].map((cell) => (
                    <th key={cell} scope="col" className="pb-2 pr-3 font-medium">
                      {cell}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-800">
                {data.items.length === 0 && (
                  <tr>
                    <td colSpan={10} className="py-3 text-ink-400">
                      Nothing in this window.
                    </td>
                  </tr>
                )}
                {data.items.map((row) => (
                  <Fragment key={row.id}>
                    <tr className="cursor-pointer text-ink-200 hover:bg-ink-900" onClick={() => setOpen(open === row.id ? null : row.id)}>
                      <td className="py-2 pr-3 font-mono text-[11px] whitespace-nowrap">
                        {/* relative: the sr-only name is absolutely placed and must stay inside the
                            table's sideways scroller. */}
                        <button type="button" aria-expanded={open === row.id} className="relative text-left">
                          {new Date(row.at).toLocaleString()}
                          <span className="sr-only">, details of {row.app_name || row.package_name || "this generation"}</span>
                        </button>
                      </td>
                      <td className="py-2 pr-3 whitespace-nowrap">
                        <span
                          aria-hidden
                          className="mr-1.5 inline-block h-1.5 w-1.5 rounded-full"
                          style={{ background: OUTCOME_COLOUR[row.outcome] ?? "gray" }}
                        />
                        {row.outcome}
                        {remarks(row) && <span className="block text-[10px] text-amber">{remarks(row)}</span>}
                      </td>
                      <td className="max-w-[180px] py-2 pr-3">
                        <span className="block truncate">{row.app_name || "—"}</span>
                        <span className="block truncate font-mono text-[11px] text-ink-400">{row.package_name}</span>
                      </td>
                      <td className="py-2 pr-3 font-mono">{row.target}</td>
                      <td className="py-2 pr-3">
                        {row.preset}
                        {(row.added.length > 0 || row.removed.length > 0) && (
                          <span className="block font-mono text-[10px] text-ink-400">
                            {row.added.length > 0 && `+${row.added.length}`} {row.removed.length > 0 && `−${row.removed.length}`}
                          </span>
                        )}
                      </td>
                      <td className="py-2 pr-3 font-mono">{row.features.length}</td>
                      <td className="py-2 pr-3 whitespace-nowrap">
                        {row.design_style} · {row.motion_style}
                        {row.glass ? " · glass" : ""}
                      </td>
                      <td className="py-2 pr-3 font-mono">{row.min_sdk ?? "—"}</td>
                      <td className="py-2 pr-3 font-mono whitespace-nowrap">{seconds(row.duration_ms)}</td>
                      <td className="py-2 font-mono text-[11px] whitespace-nowrap">
                        {row.country || "—"} · {row.device_class || "—"} · {row.client}
                      </td>
                    </tr>
                    {open === row.id && (
                      <tr>
                        <td colSpan={10} className="bg-ink-900 p-3">
                          {row.failure_reason && (
                            <p className="mb-2 text-xs text-rose">
                              {row.failure_field && <span className="font-mono">{row.failure_field}: </span>}
                              {row.failure_reason}
                            </p>
                          )}
                          <p className="mb-2 font-mono text-[11px] text-ink-400">{facts(row).join(" · ")}</p>
                          <p className="mb-2 font-mono text-[11px] text-ink-400">
                            queue {optionalSeconds(row.queue_ms)} · generator {optionalSeconds(row.python_ms)} · stream {optionalSeconds(row.stream_ms)}
                          </p>
                          <Remarks title="Warnings" items={row.warnings} className="text-amber" />
                          <Remarks title="Ignored fields" items={row.ignored_fields} className="font-mono text-ink-300" />
                          <Remarks title="Notes" items={row.notes} className="text-ink-300" />
                          <pre className="max-h-96 overflow-auto rounded-lg bg-ink-950 p-3 font-mono text-[11px] leading-relaxed text-ink-300">
                            {JSON.stringify(row.spec, null, 2)}
                          </pre>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-3 flex justify-between">
            <Button
              variant="ghost"
              size="small"
              onClick={() => setPaging({ query, cursors: cursors.slice(0, -1) })}
              disabled={cursors.length === 0}
              focusableWhenDisabled
            >
              Newer
            </Button>
            <Button
              variant="ghost"
              size="small"
              onClick={() => {
                const next = data.next_before;
                if (next !== null) setPaging({ query, cursors: [...cursors, next] });
              }}
              disabled={data.next_before === null}
              focusableWhenDisabled
            >
              Older
            </Button>
          </div>
        </>
      )}
    </Panel>
  );
}

function optionalSeconds(ms: number | null): string {
  return ms === null ? "—" : seconds(ms);
}

function facts(row: Generation): string[] {
  return [
    `#${row.id}`,
    row.architecture,
    row.languages.join(", ") || "en",
    `modules ${row.feature_modules.join(", ") || "none"}`,
    `config ${optionalSeconds(row.config_ms)}`,
    `zip ${row.zip_bytes === null ? "—" : `${Math.round(row.zip_bytes / 1024)} KB`}`,
    row.cold_start ? "cold start" : "warm",
    row.generator_commit.slice(0, 7),
  ];
}

function remarks(row: Generation): string {
  const parts: string[] = [];
  if (row.warnings.length > 0) parts.push(`${row.warnings.length} warning${row.warnings.length === 1 ? "" : "s"}`);
  if (row.ignored_fields.length > 0) parts.push(`${row.ignored_fields.length} ignored`);
  return parts.join(" · ");
}

function Remarks({ title, items, className }: { title: string; items: string[]; className: string }) {
  if (items.length === 0) return null;
  return (
    <div className="mb-2 text-xs">
      <p className="font-medium text-ink-400">{title}</p>
      <ul className={`mt-1 list-disc space-y-0.5 pl-4 ${className}`}>
        {items.map((item, index) => (
          <li key={index} className="whitespace-pre-wrap break-words">
            {item}
          </li>
        ))}
      </ul>
    </div>
  );
}
