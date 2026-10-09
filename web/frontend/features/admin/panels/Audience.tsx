"use client";

import { type Audience as AudienceData, type Durations as DurationsData, useAdminQuery } from "../api.ts";
import { BarList, percent, seconds } from "../charts.tsx";
import { Panel, type PanelProps, Table } from "../Panel.tsx";

const BUCKETS: [string, string][] = [
  ["countries", "Countries"],
  ["regions", "Regions"],
  ["cities", "Cities (5 sessions or more)"],
  ["sources", "Sources"],
  ["browsers", "Browsers"],
  ["os", "Systems"],
  ["device_classes", "Devices"],
  ["viewports", "Viewports"],
  ["returning", "Returning"],
];

export function Audience({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<AudienceData>("audience", token, query, version, onUnauthorized);
  return (
    <Panel title="Audience" note="sessions, and how many of them generated" result={result}>
      {(data) => (
        <div className="space-y-6">
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {BUCKETS.map(([key, label]) => (
              <div key={key}>
                <h3 className="mb-2 text-xs font-medium text-ink-300">{label}</h3>
                <BarList
                  rows={(data.buckets[key] ?? []).slice(0, 8).map((bucket) => ({
                    key: `${bucket.country ?? ""}${bucket.key}`,
                    label: bucket.country ? `${bucket.key}, ${bucket.country}` : bucket.key || "unknown",
                    value: bucket.sessions,
                    note: bucket.sessions ? percent(bucket.generated / bucket.sessions) : undefined,
                  }))}
                />
              </div>
            ))}
          </div>
          <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
            <div>
              <h3 className="mb-2 text-xs font-medium text-ink-300">Campaigns</h3>
              <Table head={["Source / medium / campaign", "Sessions", "Generated"]} empty={data.campaigns.length === 0}>
                {data.campaigns.map((row) => (
                  <tr key={`${row.source}/${row.medium}/${row.campaign}`} className="text-ink-200">
                    <td className="py-1.5 pr-3 font-mono">{[row.source, row.medium, row.campaign].map((part) => part || "—").join(" / ")}</td>
                    <td className="py-1.5 text-right font-mono">{row.sessions}</td>
                    <td className="py-1.5 text-right font-mono">{row.generated}</td>
                  </tr>
                ))}
              </Table>
            </div>
            <div>
              <h3 className="mb-2 text-xs font-medium text-ink-300">Generations by client</h3>
              <BarList rows={data.clients.map((row) => ({ key: row.key, label: row.key, value: row.n }))} />
            </div>
          </div>
        </div>
      )}
    </Panel>
  );
}

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
              <BarList rows={data.config_buckets.map((bucket) => ({ key: String(bucket.up_to_ms), label: `< ${seconds(bucket.up_to_ms)}`, value: bucket.n }))} />
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
