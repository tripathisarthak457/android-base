"use client";

import { useId, useState } from "react";
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

const WEEKLY: [string, string][] = [["features", "Features"], ...DIMENSIONS];

/**
 * Every feature of the catalogue, zeros included, from the projects that were generated: how
 * many had it, and how many of those added it on top of their preset rather than getting it with it.
 */
export function Adoption({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<AdoptionData>("adoption", token, query, version, onUnauthorized);
  const [chosen, setChosen] = useState<string | null>(null);
  const targetName = useId();
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
        const count = (row: string, week: string) => weekly.get(week)?.[row] ?? 0;
        const share = (row: string, week: string) => {
          const all = total.get(week) ?? 0;
          return all ? count(row, week) / all : 0;
        };
        const dimensionLabel = WEEKLY.find(([key]) => key === dimension)?.[1] ?? dimension;
        const rows =
          dimension === "features"
            ? features.map((feature) => ({ key: feature.key, label: feature.title || feature.key }))
            : (target.dimensions[dimension] ?? []).map((row) => ({ key: row.key, label: row.key || "none" }));
        return (
          <div className="space-y-6">
            {data.targets.length > 1 && (
              <div role="radiogroup" aria-label="Target" className="flex flex-wrap gap-1">
                {data.targets.map((entry) => (
                  <label
                    key={entry.target}
                    className={`cursor-pointer rounded-md px-2.5 py-1 text-xs font-medium has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent ${
                      entry.target === target.target ? "bg-ink-100 text-ink-950" : "text-ink-300 hover:bg-ink-800"
                    }`}
                  >
                    <input
                      type="radio"
                      name={targetName}
                      checked={entry.target === target.target}
                      onChange={() => setChosen(entry.target)}
                      className="sr-only"
                    />
                    {entry.target} · {entry.total}
                  </label>
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
                  {WEEKLY.map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <Heatmap rows={rows} columns={weeks} count={count} share={share} />
              {rows.length > 0 && weeks.length > 0 && (
                <details className="mt-3">
                  <summary className="cursor-pointer text-xs font-medium text-ink-300 hover:text-ink-100">Numbers per week</summary>
                  <div className="mt-2">
                    <Table head={[dimensionLabel, ...weeks.map((week) => `Week of ${week}`)]}>
                      {rows.map((row) => (
                        <tr key={row.key} className="text-ink-200">
                          <td className="py-1.5 pr-3">{row.label}</td>
                          {weeks.map((week) => (
                            <td key={week} className="whitespace-nowrap py-1.5 pl-3 text-right font-mono">
                              {count(row.key, week)} <span className="text-ink-400">({percent(share(row.key, week))})</span>
                            </td>
                          ))}
                        </tr>
                      ))}
                    </Table>
                  </div>
                </details>
              )}
            </div>
          </div>
        );
      }}
    </Panel>
  );
}
