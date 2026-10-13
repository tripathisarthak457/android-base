"use client";

import { type Failures as FailuresData, type Rate, useAdminQuery } from "../api.ts";
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
