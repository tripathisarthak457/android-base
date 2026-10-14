"use client";

import { type Audience as AudienceData, useAdminQuery } from "../api.ts";
import { BarList, percent } from "../charts.tsx";
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
