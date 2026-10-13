"use client";

import { useState } from "react";
import { type Adoption as AdoptionData, useAdminQuery } from "../api.ts";
import { BarList, Heatmap, percent } from "../charts.tsx";
import { Panel, type PanelProps, Table } from "../Panel.tsx";

const DIMENSIONS: [string, string][] = [
  ["presets", "Nearest preset"],
  ["architectures", "Architecture"],
  ["design_styles", "Design style"],
  ["motion_styles", "Motion style"],
  ["languages", "Languages"],
];

/**
 * Every feature of the catalogue, zeros included, from the projects that were generated: how
 * many had it, and how many of those added it on top of their preset rather than getting it with it.
 */
export function Adoption({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<AdoptionData>("adoption", token, query, version, onUnauthorized);
  const [chosen, setChosen] = useState<string | null>(null);
  const [dimension, setDimension] = useState("features");
  return (
    <Panel title="Adoption" note="generated projects only" result={result}>
      {(data) => {
        const target = data.targets.find((entry) => entry.target === chosen) ?? data.targets[0];
        if (!target) return <p className="text-sm text-ink-400">No targets.</p>;
        const weeks = target.weekly.map((week) => week.week);
        const total = new Map(target.weekly.map((week) => [week.week, week.total]));
        const weekly = new Map(target.weekly.map((week) => [week.week, week.values[dimension] ?? {}]));
        const features = [...target.features].sort((a, b) => b.n - a.n);
        const rows =
          dimension === "features"
            ? features.map((feature) => ({ key: feature.key, label: feature.title || feature.key }))
            : (target.dimensions[dimension] ?? []).map((row) => ({ key: row.key, label: row.key || "none" }));
        return (
          <div className="space-y-6">
            {data.targets.length > 1 && (
              <div className="flex gap-1">
                {data.targets.map((entry) => (
                  <button
                    key={entry.target}
                    type="button"
                    aria-pressed={entry.target === target.target}
                    onClick={() => setChosen(entry.target)}
                    className={`rounded-md px-2.5 py-1 text-xs font-medium ${entry.target === target.target ? "bg-ink-700 text-ink-100" : "text-ink-300 hover:bg-ink-800"}`}
                  >
                    {entry.target} · {entry.total}
                  </button>
                ))}
              </div>
            )}
            <div className="grid gap-6 lg:grid-cols-2">
              <Table head={["Feature", "Projects", "Share", "Added by hand"]} empty={features.length === 0}>
                {features.map((feature) => (
                  <tr key={feature.key} className={feature.n === 0 ? "text-ink-400" : "text-ink-200"}>
                    <td className="py-1.5 pr-3" title={feature.key}>
                      {feature.title || feature.key}
                    </td>
                    <td className="py-1.5 text-right font-mono">{feature.n}</td>
                    <td className="py-1.5 text-right font-mono">{percent(feature.share)}</td>
                    <td className="py-1.5 text-right font-mono">{feature.picked}</td>
                  </tr>
                ))}
              </Table>
              <div className="space-y-5">
                {DIMENSIONS.map(([key, label]) => (
                  <div key={key}>
                    <h3 className="mb-2 text-xs font-medium text-ink-300">{label}</h3>
                    <BarList rows={(target.dimensions[key] ?? []).map((row) => ({ key: row.key, label: row.key || "none", value: row.n }))} />
                  </div>
                ))}
              </div>
            </div>
            <div>
              <div className="mb-2 flex flex-wrap items-center gap-3">
                <h3 className="text-xs font-medium text-ink-300">Weekly share of projects</h3>
                <select
                  aria-label="Weekly share of"
                  value={dimension}
                  onChange={(event) => setDimension(event.target.value)}
                  className="h-7 rounded-md border border-ink-500 bg-ink-900 px-2 text-xs text-ink-100"
                >
                  {[["features", "Features"], ...DIMENSIONS].map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <Heatmap
                rows={rows}
                columns={weeks}
                count={(row, column) => weekly.get(column)?.[row] ?? 0}
                share={(row, column) => {
                  const all = total.get(column) ?? 0;
                  return all ? (weekly.get(column)?.[row] ?? 0) / all : 0;
                }}
              />
            </div>
          </div>
        );
      }}
    </Panel>
  );
}
