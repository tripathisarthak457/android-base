"use client";

import { type Health as HealthData, useAdminQuery } from "../api.ts";
import { percent } from "../charts.tsx";
import { Panel, type PanelProps, Table } from "../Panel.tsx";

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
