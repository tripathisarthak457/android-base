"use client";

import type { ReactNode } from "react";
import { OUTCOMES } from "./filters.ts";

/** Hand-drawn SVG and bar charts, in the admin chunk only: no charting library for a few small multiples. */

export const OUTCOME_COLOUR: Record<string, string> = {
  ok: "var(--color-mint)",
  download_interrupted: "var(--color-amber)",
  invalid: "var(--color-ink-500)",
  internal_error: "var(--color-rose)",
  timeout: "color-mix(in oklab, var(--color-rose) 60%, black)",
  abandoned_queue: "var(--color-ink-600)",
  abandoned_running: "var(--color-ink-700)",
  rate_limited: "color-mix(in oklab, var(--color-amber) 55%, var(--color-ink-500))",
};

export function percent(value: number, digits = 0): string {
  return `${(value * 100).toFixed(digits)}%`;
}

export function seconds(ms: number): string {
  return ms >= 60_000 ? `${(ms / 60_000).toFixed(1)} min` : `${(ms / 1000).toFixed(1)} s`;
}

export function Legend({ items }: { items: { label: string; colour: string; line?: boolean }[] }) {
  return (
    <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-ink-300">
      {items.map((item) => (
        <li key={item.label} className="flex items-center gap-1.5">
          <span aria-hidden className={item.line ? "h-0.5 w-3" : "h-2 w-2 rounded-sm"} style={{ background: item.colour }} />
          {item.label}
        </li>
      ))}
    </ul>
  );
}

/** Horizontal bars, one per row, scaled to the largest. */
export function BarList({ rows, format = String, empty = "Nothing in this window." }: { rows: { label: ReactNode; value: number; note?: string; key: string }[]; format?: (value: number) => string; empty?: string }) {
  if (rows.length === 0) return <p className="text-sm text-ink-400">{empty}</p>;
  const top = Math.max(1, ...rows.map((row) => row.value));
  return (
    <ul className="space-y-1.5">
      {rows.map((row) => (
        <li key={row.key} className="flex items-center gap-2.5">
          <span className="w-32 shrink-0 truncate font-mono text-[11px] text-ink-300">{row.label}</span>
          <span className="relative h-4 flex-1 overflow-hidden rounded bg-ink-800">
            <span className="absolute inset-y-0 left-0 rounded bg-accent/60" style={{ width: `${(row.value / top) * 100}%` }} />
          </span>
          <span className="w-20 shrink-0 text-right font-mono text-[11px] text-ink-300">
            {format(row.value)}
            {row.note && <span className="text-ink-400"> {row.note}</span>}
          </span>
        </li>
      ))}
    </ul>
  );
}

/** Generations per day stacked by outcome, with visitors as a line on the same scale. */
export function DailyChart({ days }: { days: { day: string; outcomes: Record<string, number>; visitors: number }[] }) {
  if (days.length === 0) return <p className="text-sm text-ink-400">Nothing in this window.</p>;
  const width = 720;
  const height = 200;
  const pad = { top: 12, right: 8, bottom: 22, left: 32 };
  const plotWidth = width - pad.left - pad.right;
  const plotHeight = height - pad.top - pad.bottom;
  const totals = days.map((day) => OUTCOMES.reduce((sum, key) => sum + (day.outcomes[key] ?? 0), 0));
  const peak = Math.max(1, ...totals, ...days.map((day) => day.visitors));
  const slot = plotWidth / days.length;
  const bar = Math.max(1.5, slot - 2);
  const y = (value: number) => pad.top + plotHeight - (value / peak) * plotHeight;
  const line = days.map((day, index) => `${index === 0 ? "M" : "L"}${(pad.left + (index + 0.5) * slot).toFixed(1)},${y(day.visitors).toFixed(1)}`).join(" ");
  const generated = totals.reduce((sum, value) => sum + value, 0);
  const visitors = days.reduce((sum, day) => sum + day.visitors, 0);

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="w-full" role="img" aria-label={`${generated} generations and ${visitors} daily visitors from ${days[0]?.day} to ${days.at(-1)?.day}`}>
      {[0, 0.5, 1].map((fraction) => (
        <g key={fraction}>
          <line x1={pad.left} x2={width - pad.right} y1={y(peak * fraction)} y2={y(peak * fraction)} stroke="var(--color-ink-700)" strokeDasharray="2 4" />
          <text x={4} y={y(peak * fraction) + 3.5} fill="var(--color-ink-400)" fontSize="9" fontFamily="monospace">
            {Math.round(peak * fraction)}
          </text>
        </g>
      ))}
      {days.map((day, index) => {
        let base = 0;
        return (
          <g key={day.day}>
            <title>{`${day.day}: ${OUTCOMES.filter((key) => day.outcomes[key]).map((key) => `${day.outcomes[key]} ${key}`).join(", ") || "no generations"}; ${day.visitors} visitors`}</title>
            {OUTCOMES.map((key) => {
              const value = day.outcomes[key] ?? 0;
              if (value === 0) return null;
              const top = y(base + value);
              const rect = <rect key={key} x={pad.left + index * slot + 1} y={top} width={bar} height={y(base) - top} fill={OUTCOME_COLOUR[key]} />;
              base += value;
              return rect;
            })}
          </g>
        );
      })}
      <path d={line} fill="none" stroke="var(--color-accent)" strokeWidth="1.75" />
      {[days[0], days.at(-1)].map((day, index) =>
        day ? (
          <text key={`${day.day}-${index}`} x={index === 0 ? pad.left : width - pad.right} y={height - 6} textAnchor={index === 0 ? "start" : "end"} fill="var(--color-ink-400)" fontSize="9" fontFamily="monospace">
            {day.day.slice(5)}
          </text>
        ) : null,
      )}
    </svg>
  );
}

/** Rows by columns, each cell shaded by its share; the numbers are in each cell's title. */
export function Heatmap({ rows, columns, share, count }: { rows: { key: string; label: string }[]; columns: string[]; share: (row: string, column: string) => number; count: (row: string, column: string) => number }) {
  if (rows.length === 0 || columns.length === 0) return <p className="text-sm text-ink-400">Nothing in this window.</p>;
  const cell = 16;
  const label = 130;
  const width = label + columns.length * (cell + 2);
  const height = 18 + rows.length * (cell + 2);
  return (
    <div className="overflow-x-auto">
      <svg width={width} height={height} role="img" aria-label={`Weekly share of ${rows.length} rows over ${columns.length} weeks`}>
        {columns.map((column, index) =>
          index % Math.ceil(columns.length / 6) === 0 ? (
            <text key={column} x={label + index * (cell + 2)} y={10} fill="var(--color-ink-400)" fontSize="9" fontFamily="monospace">
              {column.slice(5)}
            </text>
          ) : null,
        )}
        {rows.map((row, rowIndex) => (
          <g key={row.key} transform={`translate(0 ${18 + rowIndex * (cell + 2)})`}>
            <text x={0} y={cell - 4} fill="var(--color-ink-300)" fontSize="10" fontFamily="monospace">
              {row.label.length > 20 ? `${row.label.slice(0, 19)}…` : row.label}
            </text>
            {columns.map((column, index) => {
              const value = share(row.key, column);
              return (
                <rect key={column} x={label + index * (cell + 2)} width={cell} height={cell} rx={2} fill="var(--color-accent)" fillOpacity={value === 0 ? 0.06 : 0.15 + value * 0.85}>
                  <title>{`${row.label}, week of ${column}: ${count(row.key, column)} (${percent(value)})`}</title>
                </rect>
              );
            })}
          </g>
        ))}
      </svg>
    </div>
  );
}
