"use client";

import { type Funnel as FunnelData, type Overview as OverviewData, type Series, useAdminQuery } from "../api.ts";
import { DailyChart, Legend, OUTCOME_COLOUR, percent, seconds } from "../charts.tsx";
import { OUTCOMES } from "../filters.ts";
import { Panel, type PanelProps } from "../Panel.tsx";

const FAILED = ["internal_error", "timeout"];
const LOST = ["abandoned_queue", "abandoned_running", "rate_limited"];

export function Headline({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<OverviewData>("overview", token, query, version, onUnauthorized);
  return (
    <Panel title="Headline" result={result}>
      {(data) => {
        const count = (keys: string[]) => keys.reduce((sum, key) => sum + (data.outcomes[key] ?? 0), 0);
        const generated = count(["ok", "download_interrupted"]);
        const reached = generated + count(FAILED);
        const cards = [
          { label: "Generations", value: data.generations.toLocaleString(), sub: `${count(["ok"])} ${count(["ok"]) === 1 ? "download" : "downloads"} finished` },
          {
            label: "Generator success",
            value: reached ? percent(generated / reached, 1) : "—",
            sub: `${count(FAILED)} generator failures`,
            tone: reached && generated / reached < 0.98 ? "bad" : undefined,
          },
          { label: "Refused specs", value: count(["invalid"]).toLocaleString(), sub: `${count(LOST)} rate-limited or abandoned` },
          { label: "Generation time", value: seconds(data.median_ms), sub: `p95 ${seconds(data.p95_ms)}` },
          { label: "Visitors", value: data.visitors.toLocaleString(), sub: `${data.sessions.toLocaleString()} sessions` },
          { label: "Returning", value: percent(data.returning_share), sub: "sessions with a saved draft" },
          { label: "Open errors", value: data.open_errors.toLocaleString(), sub: data.open_errors ? "need a look" : "nothing broken", tone: data.open_errors ? "bad" : "good" },
          { label: "New reports", value: data.new_feedback.toLocaleString(), sub: "waiting for triage" },
        ];
        return (
          <dl className="grid grid-cols-2 gap-3 md:grid-cols-4">
            {cards.map((card) => (
              <div key={card.label} className="rounded-lg border border-ink-700 bg-ink-900 p-4">
                <dt className="text-xs text-ink-400">{card.label}</dt>
                <dd className={`mt-1 font-mono text-2xl font-bold ${card.tone === "bad" ? "text-rose" : card.tone === "good" ? "text-mint" : "text-ink-100"}`}>{card.value}</dd>
                <dd className="mt-0.5 text-xs text-ink-400">{card.sub}</dd>
              </div>
            ))}
          </dl>
        );
      }}
    </Panel>
  );
}

const STEP_LABEL: Record<string, string> = {
  platform_selected: "Platform chosen",
  "step_viewed:identity": "Project",
  "step_viewed:features": "Features",
  "step_viewed:look": "Look",
  "step_viewed:motion": "Motion",
  "step_viewed:build": "Build",
  "step_viewed:review": "Review",
  generate_clicked: "Generate clicked",
  generated: "Generated",
  downloaded: "Downloaded",
};

export function Funnel({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<FunnelData>("funnel", token, query, version, onUnauthorized);
  return (
    <Panel title="Funnel" note="distinct sessions per step" result={result}>
      {(data) => (
        <div className="grid gap-6 lg:grid-cols-2">
          {data.targets.map((target) => {
            const rows = [{ key: "landed", sessions: data.landed }, ...target.steps];
            return (
              <div key={target.target}>
                <h3 className="mb-2 font-mono text-xs text-ink-300">{target.target}</h3>
                <ol className="space-y-1.5">
                  {rows.map((row, index) => {
                    const previous = index === 0 ? row.sessions : (rows[index - 1]?.sessions ?? 0);
                    const drop = previous > 0 && index > 0 ? 1 - row.sessions / previous : 0;
                    return (
                      <li key={row.key} className="flex items-center gap-2.5">
                        <span className="w-28 shrink-0 text-xs text-ink-300">{row.key === "landed" ? "Landed (all)" : (STEP_LABEL[row.key] ?? row.key)}</span>
                        <span className="relative h-5 flex-1 overflow-hidden rounded bg-ink-800">
                          <span className="absolute inset-y-0 left-0 rounded bg-accent/30" style={{ width: `${data.landed ? (row.sessions / data.landed) * 100 : 0}%` }} />
                          <span className="relative px-2 font-mono text-[11px] leading-5 text-ink-100">{row.sessions}</span>
                        </span>
                        <span className="w-12 shrink-0 text-right font-mono text-[11px] text-ink-400">{index === 0 || drop <= 0 ? "" : `−${percent(drop)}`}</span>
                      </li>
                    );
                  })}
                </ol>
                {Object.keys(target.also).length > 0 && (
                  <p className="mt-2 font-mono text-[11px] text-ink-400">
                    {Object.entries(target.also)
                      .map(([key, n]) => `${key} ${n}`)
                      .join(" · ")}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}
    </Panel>
  );
}

export function Daily({ token, query, version, onUnauthorized }: PanelProps) {
  const result = useAdminQuery<Series>("series", token, query, version, onUnauthorized);
  return (
    <Panel title="Every day" result={result}>
      {(data) => (
        <>
          <Legend items={[...OUTCOMES.map((key) => ({ label: key, colour: OUTCOME_COLOUR[key] ?? "gray" })), { label: "visitors", colour: "var(--color-accent)", line: true }]} />
          <div className="mt-3">
            <DailyChart days={data.days} />
          </div>
        </>
      )}
    </Panel>
  );
}
