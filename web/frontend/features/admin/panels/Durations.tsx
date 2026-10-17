"use client";

import { type Durations as DurationsData, useAdminQuery } from "../api.ts";
import { BarList, seconds } from "../charts.tsx";
import { Panel, type PanelProps, Table } from "../Panel.tsx";

export function Durations({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<DurationsData>("durations", token, query, version, onUnauthorized);
  return (
    <Panel title="Durations" result={result}>
      {(data) => {
        const timings: [string, typeof data.config][] = [
          ["In the configurator", data.config],
          ["Waiting for a slot", data.queue],
          ["Downloading", data.download],
        ];
        return (
          <div className="grid gap-6 lg:grid-cols-3">
            <dl className="space-y-3">
              {timings.map(([label, timing]) => (
                <div key={label} className="rounded-lg border border-ink-700 bg-ink-900 p-3">
                  <dt className="text-xs text-ink-400">{label}</dt>
                  <dd className="mt-0.5 font-mono text-sm text-ink-100">
                    {timing.n ? `${seconds(timing.p50_ms)} median · p95 ${seconds(timing.p95_ms)}` : "—"}
                  </dd>
                  <dd className="font-mono text-[11px] text-ink-400">{timing.n} measured</dd>
                </div>
              ))}
            </dl>
            <div>
              <h3 className="mb-2 text-xs font-medium text-ink-300">Time in the configurator</h3>
              <BarList
                rows={data.config_buckets.map((bucket) => ({ key: String(bucket.up_to_ms), label: `< ${seconds(bucket.up_to_ms)}`, value: bucket.n }))}
              />
            </div>
            <div>
              <h3 className="mb-2 text-xs font-medium text-ink-300">Generation time by feature count</h3>
              <Table head={["Features", "Median", "p95", "n"]} empty={data.generation_by_feature_count.length === 0}>
                {data.generation_by_feature_count.map((row) => (
                  <tr key={row.bucket} className="text-ink-200">
                    <td className="py-1.5 pr-3 font-mono">{row.bucket}</td>
                    <td className="py-1.5 text-right font-mono">{row.n ? seconds(row.p50_ms) : "—"}</td>
                    <td className="py-1.5 text-right font-mono">{row.n ? seconds(row.p95_ms) : "—"}</td>
                    <td className="py-1.5 text-right font-mono">{row.n}</td>
                  </tr>
                ))}
              </Table>
            </div>
          </div>
        );
      }}
    </Panel>
  );
}
