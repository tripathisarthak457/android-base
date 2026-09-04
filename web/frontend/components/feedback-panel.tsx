"use client";

/**
 * Reports from people, as opposed to `errors`, which is what the software noticed about itself.
 *
 * A row collapses to a title; expanding shows everything the reporter typed *and* everything the
 * site attached. That second half is the reason this beats a GitHub issue template — the exact
 * feature set, package name, motion style and browser arrive without anybody being asked for
 * them, which is usually the difference between reproducing a bug today and three emails later.
 */

import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";
import { Badge, Button, Card } from "./primitives";

export type Feedback = {
  id: number;
  createdAt: string;
  kind: string;
  severity: string;
  area: string;
  title: string;
  body: string;
  steps: string;
  expected: string;
  actual: string;
  appName: string;
  packageName: string;
  features: string[];
  preset: string;
  minSdk: number;
  motionStyle: string;
  fontName: string;
  accentColour: string;
  contact: string;
  userAgent: string;
  pageUrl: string;
  status: string;
  notes: string;
};

export type FeedbackCounts = {
  new: number;
  blocking: number;
  total: number;
  last7Days: number;
};

const STATUSES = ["new", "triaged", "fixed", "wontfix"] as const;

const KIND_TONE: Record<string, "neutral" | "accent" | "mint" | "amber"> = {
  bug: "amber",
  idea: "accent",
  praise: "mint",
  question: "neutral",
};

export function FeedbackPanel({
  items,
  counts,
  onUpdate,
}: {
  items: Feedback[];
  counts: FeedbackCounts | null;
  onUpdate: (id: number, status: string, notes: string) => void;
}) {
  const [expanded, setExpanded] = useState<number | null>(null);
  const [filter, setFilter] = useState<string>("new");

  const shown = items.filter((item) => filter === "all" || item.status === filter);

  return (
    <Card className="mt-6 p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-sm font-semibold text-ink-200">Reports</h2>
          <p className="mt-1 text-xs text-ink-500">
            {counts
              ? `${counts.new} new · ${counts.blocking} blocking · ${counts.last7Days} this week · ${counts.total} all time`
              : "Sent from the report button on the site."}
          </p>
        </div>
        <div className="flex gap-1">
          {[...STATUSES, "all"].map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setFilter(option)}
              className={`rounded-md px-2.5 py-1 text-xs font-medium capitalize transition-colors ${
                filter === option
                  ? "bg-ink-700 text-ink-100"
                  : "text-ink-400 hover:bg-ink-800 hover:text-ink-200"
              }`}
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      {shown.length === 0 ? (
        <p className="mt-5 rounded-lg border border-ink-700 bg-ink-900 p-4 text-sm text-ink-500">
          Nothing {filter === "all" ? "yet" : `marked ${filter}`}.
        </p>
      ) : (
        <div className="mt-4 divide-y divide-ink-800">
          {shown.map((item) => (
            <div key={item.id} className="py-3">
              <div className="flex items-start gap-3">
                <Badge tone={KIND_TONE[item.kind] ?? "neutral"}>{item.kind}</Badge>
                {item.severity === "blocks" && <Badge tone="amber">blocks</Badge>}

                <button
                  type="button"
                  className="min-w-0 flex-1 text-left"
                  onClick={() => setExpanded(expanded === item.id ? null : item.id)}
                >
                  <p className="truncate text-sm font-medium text-ink-100">{item.title}</p>
                  <p className="mt-0.5 font-mono text-[11px] text-ink-500">
                    #{item.id} · {item.area || "unspecified"} ·{" "}
                    {new Date(item.createdAt).toLocaleString()}
                    {item.contact ? " · has contact" : ""}
                  </p>
                </button>

                <select
                  value={item.status}
                  onChange={(event) => onUpdate(item.id, event.target.value, "")}
                  aria-label="Status"
                  className="shrink-0 rounded-md border border-ink-600 bg-ink-900 px-2 py-1 text-xs text-ink-300"
                >
                  {STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </div>

              <AnimatePresence>
                {expanded === item.id && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="overflow-hidden"
                  >
                    <div className="mt-3 grid gap-5 rounded-lg bg-ink-900 p-4 lg:grid-cols-2">
                      <div className="space-y-3 text-xs">
                        <Detail label="What happened" value={item.body} mono={false} />
                        {item.steps ? <Detail label="Steps" value={item.steps} /> : null}
                        {item.expected ? <Detail label="Expected" value={item.expected} /> : null}
                        {item.actual ? <Detail label="Actual" value={item.actual} /> : null}
                        {item.contact ? <Detail label="Contact" value={item.contact} /> : null}
                      </div>

                      <div className="space-y-2 text-xs">
                        <p className="font-medium text-ink-300">What they had configured</p>
                        <dl className="grid gap-1">
                          {(
                            [
                              ["App", item.appName],
                              ["Package", item.packageName],
                              ["Preset", item.preset],
                              ["Min SDK", item.minSdk ? String(item.minSdk) : ""],
                              ["Typeface", item.fontName],
                              ["Motion", item.motionStyle],
                              ["Accent", item.accentColour],
                            ] as [string, string][]
                          )
                            .filter(([, value]) => value)
                            .map(([label, value]) => (
                              <div key={label} className="flex justify-between gap-3">
                                <dt className="text-ink-500">{label}</dt>
                                <dd className="truncate font-mono text-ink-300">{value}</dd>
                              </div>
                            ))}
                        </dl>

                        {item.features.length > 0 && (
                          <div className="flex flex-wrap gap-1 pt-1">
                            {item.features.map((feature) => (
                              <span
                                key={feature}
                                className="rounded border border-ink-700 px-1.5 py-0.5 font-mono text-[10px] text-ink-400"
                              >
                                {feature}
                              </span>
                            ))}
                          </div>
                        )}

                        {item.userAgent ? (
                          <p className="border-t border-ink-800 pt-2 font-mono text-[10px] leading-relaxed text-ink-500">
                            {item.userAgent}
                          </p>
                        ) : null}
                        {item.pageUrl ? (
                          <p className="truncate font-mono text-[10px] text-ink-500">
                            {item.pageUrl}
                          </p>
                        ) : null}
                      </div>
                    </div>

                    <NoteBox current={item.notes} onSave={(notes) => onUpdate(item.id, "", notes)} />
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

function Detail({ label, value, mono = true }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <p className="text-ink-500">{label}</p>
      <p
        className={`mt-0.5 whitespace-pre-wrap text-ink-200 ${mono ? "font-mono text-[11px]" : ""}`}
      >
        {value}
      </p>
    </div>
  );
}

function NoteBox({ current, onSave }: { current: string; onSave: (notes: string) => void }) {
  const [value, setValue] = useState(current);

  return (
    <div className="mt-3 flex gap-2">
      <input
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder="Triage note — what you found, what you did"
        className="h-9 flex-1 rounded-lg border border-ink-600 bg-ink-900 px-3 text-xs text-ink-100 placeholder:text-ink-500 focus:border-accent focus:outline-none"
      />
      <Button
        variant="secondary"
        size="small"
        onClick={() => onSave(value)}
        disabled={value === current || value.trim().length === 0}
      >
        Save note
      </Button>
    </div>
  );
}
