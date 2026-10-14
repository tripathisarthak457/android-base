"use client";

import { type ReactNode, createContext, useContext, useEffect, useId } from "react";
import { Spinner } from "../../components/ui.tsx";
import type { DateWindow, Query } from "./api.ts";

/** What every panel is given: the token, the shared query, and a counter that forces a refetch. */
export type PanelProps = { token: string; query: string; version: number; onUnauthorized: () => void };

/**
 * Where a panel says its load failed, so the page can announce failures once. When the API is
 * down every panel fails at the same moment, and an alert in each would read the same sentence
 * eleven times.
 */
export const PanelFailures = createContext<(panel: string, message: string | null) => void>(() => undefined);

/**
 * One admin card. It states the window its numbers cover, and when a refetch fails it keeps
 * the last numbers on screen under the error instead of going blank.
 */
export function Panel<T extends DateWindow>({
  title,
  note,
  result,
  actions,
  className = "",
  children,
}: {
  title: string;
  note?: string;
  result: Query<T>;
  actions?: ReactNode;
  className?: string;
  children: (data: T) => ReactNode;
}) {
  const { data, error, loading } = result;
  const headingId = useId();
  const report = useContext(PanelFailures);
  const message = error?.message ?? null;
  useEffect(() => {
    report(headingId, message);
  }, [report, headingId, message]);
  useEffect(() => () => report(headingId, null), [report, headingId]);
  return (
    <section aria-labelledby={headingId} className={`min-w-0 rounded-xl border border-ink-700 bg-ink-850 p-5 ${className}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 id={headingId} className="text-sm font-semibold text-ink-100">
            {title}
          </h2>
          <p className="mt-1 text-xs text-ink-400">
            {data ? `${data.from} to ${data.to}` : " "}
            {note && ` · ${note}`}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {loading && <Spinner className="text-ink-400" />}
          {actions}
        </div>
      </div>
      {error && (
        <p className="mt-3 rounded-lg border border-amber/30 bg-amber/10 px-3 py-2 text-xs text-amber">
          {error.message}
          {data ? " Showing the last numbers that loaded." : ""}
        </p>
      )}
      <div className="mt-4">{data ? children(data) : !error && <p className="text-sm text-ink-400">Loading…</p>}</div>
    </section>
  );
}

export function Table({ head, children, empty }: { head: ReactNode[]; children: ReactNode; empty?: boolean }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs">
        <thead>
          <tr className="text-ink-400">
            {head.map((cell, index) => (
              <th key={index} scope="col" className={`pb-2 font-medium ${index > 0 ? "text-right" : ""}`}>
                {cell}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-800">
          {empty ? (
            <tr>
              <td colSpan={head.length} className="py-3 text-ink-400">
                Nothing in this window.
              </td>
            </tr>
          ) : (
            children
          )}
        </tbody>
      </table>
    </div>
  );
}
