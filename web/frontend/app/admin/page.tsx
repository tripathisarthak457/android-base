"use client";

/**
 * The admin portal.
 *
 * Deliberately one page. Every number the generator produces fits on a screen, and splitting it
 * across five routes would mean five loads to answer "is anything broken" — which is the only
 * question this exists to answer quickly.
 *
 * The token is held in `sessionStorage`, not a cookie: it is a bearer token for a read-only API
 * that one person uses, and a cookie would have to be scoped, secured and cleared on a domain
 * this page does not own. Closing the tab logs you out, which for an admin panel is correct.
 */

import { AnimatePresence, motion } from "framer-motion";
import { useCallback, useEffect, useState } from "react";
import {
  FeedbackPanel,
  type Feedback,
  type FeedbackCounts,
} from "../../components/feedback-panel";
import { Badge, Button, Card, Field, Spinner, TextInput } from "../../components/primitives";
import { API_BASE } from "../../lib/api";

type Overview = {
  generationsTotal: number;
  generationsToday: number;
  generations30Days: number;
  successRate: number;
  uniqueVisitors30Days: number;
  uniqueVisitorsToday: number;
  medianDurationMs: number;
  p95DurationMs: number;
  medianZipBytes: number;
  openErrors: number;
  funnel: Record<string, number>;
};

type DayPoint = { day: string; total: number; failed: number; visitors: number };
type FeatureCount = { feature: string; count: number; share: number };
type ErrorGroup = {
  id: number;
  fingerprint: string;
  kind: string;
  message: string;
  detail: string;
  path: string;
  occurrences: number;
  firstSeen: string;
  lastSeen: string;
  resolved: boolean;
};
type Recent = {
  at: string;
  appName: string;
  packageName: string;
  features: string[];
  succeeded: boolean;
  durationMs: number;
  zipBytes: number;
  failure: string;
};
type RouteHealth = {
  route: string;
  requests: number;
  errorRate: number;
  medianMs: number;
  p95Ms: number;
};

const TOKEN_KEY = "generator-admin-token";

export default function Admin() {
  const [token, setToken] = useState("");
  const [authed, setAuthed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);

  const [overview, setOverview] = useState<Overview | null>(null);
  const [daily, setDaily] = useState<DayPoint[]>([]);
  const [features, setFeatures] = useState<FeatureCount[]>([]);
  const [errors, setErrors] = useState<ErrorGroup[]>([]);
  const [recent, setRecent] = useState<Recent[]>([]);
  const [health, setHealth] = useState<RouteHealth[]>([]);
  const [feedback, setFeedback] = useState<Feedback[]>([]);
  const [feedbackCounts, setFeedbackCounts] = useState<FeedbackCounts | null>(null);
  const [showResolved, setShowResolved] = useState(false);

  const load = useCallback(
    async (bearer: string, includeResolved: boolean) => {
      setLoading(true);
      setFailure(null);
      const headers = { Authorization: `Bearer ${bearer}` };

      try {
        const paths = [
          "/api/admin/overview",
          "/api/admin/daily?days=30",
          "/api/admin/features",
          `/api/admin/errors?resolved=${includeResolved}`,
          "/api/admin/generations?limit=40",
          "/api/admin/health",
          "/api/admin/feedback?limit=100",
        ];
        const responses = await Promise.all(
          paths.map((path) => fetch(`${API_BASE}${path}`, { headers })),
        );

        if (responses.some((response) => response.status === 401)) {
          throw new Error("That token was not accepted.");
        }
        const bad = responses.find((response) => !response.ok);
        if (bad) throw new Error(`The API returned ${bad.status}.`);

        const [o, d, f, e, r, h, fb] = await Promise.all(
          responses.map((response) => response.json()),
        );
        setOverview(o);
        setDaily(d ?? []);
        setFeatures(f ?? []);
        setErrors(e ?? []);
        setRecent(r ?? []);
        setHealth(h ?? []);
        setFeedback(fb?.items ?? []);
        setFeedbackCounts(fb?.counts ?? null);
        setAuthed(true);
        sessionStorage.setItem(TOKEN_KEY, bearer);
      } catch (error) {
        setFailure(
          error instanceof Error
            ? error.message
            : "Could not reach the API. It records nothing when DATABASE_URL is unset, and the " +
              "admin routes are not registered at all in that case.",
        );
        setAuthed(false);
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    const saved = sessionStorage.getItem(TOKEN_KEY);
    if (saved) {
      setToken(saved);
      void load(saved, false);
    }
  }, [load]);

  if (!authed) {
    return (
      <main className="flex min-h-screen items-center justify-center px-6">
        <Card className="w-full max-w-sm p-7">
          <h1 className="text-lg font-semibold text-ink-100">Admin</h1>
          <p className="mt-1.5 text-sm text-ink-400">
            The token is <code className="text-ink-300">ADMIN_TOKEN</code> from the API&apos;s
            environment.
          </p>
          <form
            className="mt-5"
            onSubmit={(event) => {
              event.preventDefault();
              void load(token, showResolved);
            }}
          >
            <Field label="Token" error={failure ?? undefined}>
              <TextInput
                type="password"
                value={token}
                invalid={Boolean(failure)}
                onChange={(event) => setToken(event.target.value)}
                placeholder="••••••••••••••••••••••••"
                autoFocus
              />
            </Field>
            <Button type="submit" className="mt-4 w-full" disabled={loading || token.length < 8}>
              {loading ? <Spinner /> : "Sign in"}
            </Button>
          </form>
        </Card>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-7xl px-6 py-10">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-ink-100">Generator admin</h1>
          <p className="mt-1 text-sm text-ink-400">
            Everything on this page is the last 30 days unless it says otherwise.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="small"
            onClick={() => void load(token, showResolved)}
            disabled={loading}
          >
            {loading ? <Spinner /> : "Refresh"}
          </Button>
          <Button
            variant="ghost"
            size="small"
            onClick={() => {
              sessionStorage.removeItem(TOKEN_KEY);
              setAuthed(false);
              setToken("");
            }}
          >
            Sign out
          </Button>
        </div>
      </header>

      {overview && <Headline overview={overview} counts={feedbackCounts} />}
      {overview && <Funnel funnel={overview.funnel} />}

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <DailyChart points={daily} />
        </div>
        <FeatureBars features={features} />
      </div>

      <FeedbackPanel
        items={feedback}
        counts={feedbackCounts}
        onUpdate={async (id, status, notes) => {
          await fetch(`${API_BASE}/api/admin/feedback/update`, {
            method: "POST",
            headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
            body: JSON.stringify({ id, status, notes }),
          });
          void load(token, showResolved);
        }}
      />

      <Errors
        errors={errors}
        showResolved={showResolved}
        onToggleResolved={(next) => {
          setShowResolved(next);
          void load(token, next);
        }}
        onResolve={async (id, resolved) => {
          await fetch(`${API_BASE}/api/admin/errors/resolve`, {
            method: "POST",
            headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
            body: JSON.stringify({ id, resolved }),
          });
          void load(token, showResolved);
        }}
      />

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <RecentTable recent={recent} />
        <HealthTable health={health} />
      </div>
    </main>
  );
}

function Headline({
  overview,
  counts,
}: {
  overview: Overview;
  counts: FeedbackCounts | null;
}) {
  const cards = [
    { label: "Projects generated", value: overview.generationsTotal.toLocaleString(), sub: `${overview.generationsToday} today` },
    { label: "Unique visitors", value: overview.uniqueVisitors30Days.toLocaleString(), sub: `${overview.uniqueVisitorsToday} today` },
    {
      label: "Success rate",
      value: `${(overview.successRate * 100).toFixed(1)}%`,
      sub: `${overview.generations30Days} in 30 days`,
      tone: overview.successRate < 0.95 ? ("bad" as const) : undefined,
    },
    { label: "Median build", value: `${(overview.medianDurationMs / 1000).toFixed(1)}s`, sub: `p95 ${(overview.p95DurationMs / 1000).toFixed(1)}s` },
    { label: "Median zip", value: `${(overview.medianZipBytes / 1024).toFixed(0)} KB`, sub: "per project" },
    {
      label: "Open errors",
      value: overview.openErrors.toString(),
      sub: overview.openErrors === 0 ? "nothing broken" : "needs a look",
      tone: overview.openErrors > 0 ? ("bad" as const) : ("good" as const),
    },
    {
      label: "New reports",
      value: (counts?.new ?? 0).toString(),
      sub: counts?.blocking ? `${counts.blocking} blocking` : `${counts?.total ?? 0} all time`,
      tone: counts?.blocking ? ("bad" as const) : undefined,
    },
  ];

  return (
    <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
      {cards.map((card, index) => (
        <motion.div
          key={card.label}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: index * 0.04 }}
          className="rounded-xl border border-ink-700 bg-ink-850 p-4"
        >
          <p className="text-xs text-ink-400">{card.label}</p>
          <p
            className={`mt-1 font-mono text-2xl font-bold ${
              card.tone === "bad" ? "text-rose" : card.tone === "good" ? "text-mint" : "text-ink-100"
            }`}
          >
            {card.value}
          </p>
          <p className="mt-0.5 text-xs text-ink-500">{card.sub}</p>
        </motion.div>
      ))}
    </div>
  );
}

const FUNNEL_STEPS: { key: string; label: string }[] = [
  { key: "landed", label: "Landed" },
  { key: "configured", label: "Configured" },
  { key: "generated", label: "Generated" },
  { key: "downloaded", label: "Downloaded" },
];

function Funnel({ funnel }: { funnel: Record<string, number> }) {
  const top = Math.max(1, funnel.landed ?? 0);

  return (
    <Card className="mt-4 p-5">
      <h2 className="text-sm font-semibold text-ink-200">Funnel</h2>
      <div className="mt-4 space-y-2.5">
        {FUNNEL_STEPS.map((step, index) => {
          const count = funnel[step.key] ?? 0;
          const share = count / top;
          const previous = index === 0 ? count : (funnel[FUNNEL_STEPS[index - 1].key] ?? 0);
          const dropped = previous > 0 ? 1 - count / previous : 0;

          return (
            <div key={step.key} className="flex items-center gap-3">
              <span className="w-24 shrink-0 text-xs text-ink-400">{step.label}</span>
              <div className="h-7 flex-1 overflow-hidden rounded-md bg-ink-800">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${Math.max(share * 100, 1.5)}%` }}
                  transition={{ duration: 0.6, delay: index * 0.08, ease: [0.16, 1, 0.3, 1] }}
                  className="flex h-full items-center bg-accent/25 px-2.5"
                >
                  <span className="font-mono text-xs text-accent-bright">{count}</span>
                </motion.div>
              </div>
              <span className="w-20 shrink-0 text-right font-mono text-xs text-ink-500">
                {index === 0 ? "—" : `-${(dropped * 100).toFixed(0)}%`}
              </span>
            </div>
          );
        })}
      </div>
    </Card>
  );
}

/**
 * The daily chart, drawn as an SVG.
 *
 * No charting library. It is a bar per day and a line for visitors; pulling in Recharts to draw
 * that would add 90KB to a page one person opens.
 */
function DailyChart({ points }: { points: DayPoint[] }) {
  if (points.length === 0) {
    return (
      <Card className="flex h-full min-h-[240px] items-center justify-center p-5">
        <p className="text-sm text-ink-500">Nothing yet.</p>
      </Card>
    );
  }

  const width = 720;
  const height = 200;
  const padding = { top: 14, right: 8, bottom: 22, left: 30 };
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;

  const peak = Math.max(1, ...points.map((point) => Math.max(point.total, point.visitors)));
  const barWidth = Math.max(2, plotWidth / points.length - 3);

  const visitorPath = points
    .map((point, index) => {
      const x = padding.left + (index + 0.5) * (plotWidth / points.length);
      const y = padding.top + plotHeight - (point.visitors / peak) * plotHeight;
      return `${index === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");

  return (
    <Card className="h-full p-5">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-ink-200">Last 30 days</h2>
        <div className="flex gap-4 text-xs">
          <span className="flex items-center gap-1.5 text-ink-400">
            <span className="h-2 w-2 rounded-sm bg-accent" /> generated
          </span>
          <span className="flex items-center gap-1.5 text-ink-400">
            <span className="h-2 w-2 rounded-sm bg-rose" /> failed
          </span>
          <span className="flex items-center gap-1.5 text-ink-400">
            <span className="h-0.5 w-3 bg-mint" /> visitors
          </span>
        </div>
      </div>

      <svg viewBox={`0 0 ${width} ${height}`} className="mt-4 w-full" role="img">
        {[0, 0.5, 1].map((fraction) => {
          const y = padding.top + plotHeight * (1 - fraction);
          return (
            <g key={fraction}>
              <line
                x1={padding.left}
                x2={width - padding.right}
                y1={y}
                y2={y}
                stroke="var(--color-ink-700)"
                strokeDasharray="2 4"
              />
              <text x={4} y={y + 3.5} fill="var(--color-ink-500)" fontSize="9" fontFamily="monospace">
                {Math.round(peak * fraction)}
              </text>
            </g>
          );
        })}

        {points.map((point, index) => {
          const x = padding.left + index * (plotWidth / points.length) + 1.5;
          const totalHeight = (point.total / peak) * plotHeight;
          const failedHeight = (point.failed / peak) * plotHeight;
          return (
            <g key={point.day}>
              <title>{`${point.day}: ${point.total} generated, ${point.failed} failed, ${point.visitors} visitors`}</title>
              <rect
                x={x}
                y={padding.top + plotHeight - totalHeight}
                width={barWidth}
                height={Math.max(totalHeight, 0)}
                fill="var(--color-accent)"
                opacity={0.75}
                rx={1.5}
              />
              {point.failed > 0 && (
                <rect
                  x={x}
                  y={padding.top + plotHeight - failedHeight}
                  width={barWidth}
                  height={failedHeight}
                  fill="var(--color-rose)"
                  rx={1.5}
                />
              )}
            </g>
          );
        })}

        <path d={visitorPath} fill="none" stroke="var(--color-mint)" strokeWidth="1.75" />

        {[points[0], points[points.length - 1]].map((point, index) => (
          <text
            key={point.day + index}
            x={index === 0 ? padding.left : width - padding.right}
            y={height - 6}
            textAnchor={index === 0 ? "start" : "end"}
            fill="var(--color-ink-500)"
            fontSize="9"
            fontFamily="monospace"
          >
            {point.day.slice(5)}
          </text>
        ))}
      </svg>
    </Card>
  );
}

function FeatureBars({ features }: { features: FeatureCount[] }) {
  return (
    <Card className="p-5">
      <h2 className="text-sm font-semibold text-ink-200">Feature popularity</h2>
      <p className="mt-1 text-xs text-ink-500">
        Share of successful projects that included each. The ones nobody picks are candidates for
        deletion.
      </p>
      <div className="mt-4 max-h-[300px] space-y-1.5 overflow-y-auto pr-1">
        {features.length === 0 && <p className="text-sm text-ink-500">Nothing yet.</p>}
        {features.map((feature, index) => (
          <div key={feature.feature} className="flex items-center gap-2.5">
            <span className="w-28 shrink-0 truncate font-mono text-[11px] text-ink-300">
              {feature.feature}
            </span>
            <div className="h-4 flex-1 overflow-hidden rounded bg-ink-800">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${Math.max(feature.share * 100, 1)}%` }}
                transition={{ duration: 0.5, delay: index * 0.02 }}
                className="h-full bg-accent/60"
              />
            </div>
            <span className="w-10 shrink-0 text-right font-mono text-[11px] text-ink-500">
              {(feature.share * 100).toFixed(0)}%
            </span>
          </div>
        ))}
      </div>
    </Card>
  );
}

function Errors({
  errors,
  showResolved,
  onToggleResolved,
  onResolve,
}: {
  errors: ErrorGroup[];
  showResolved: boolean;
  onToggleResolved: (next: boolean) => void;
  onResolve: (id: number, resolved: boolean) => void;
}) {
  const [expanded, setExpanded] = useState<number | null>(null);

  return (
    <Card className="mt-6 p-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-sm font-semibold text-ink-200">Errors</h2>
          <p className="mt-1 text-xs text-ink-500">
            Grouped by fingerprint, so one bug is one row however many times it fired. Rejections
            caused by what a visitor typed are not in here.
          </p>
        </div>
        <Button
          variant="ghost"
          size="small"
          onClick={() => onToggleResolved(!showResolved)}
        >
          {showResolved ? "Hide resolved" : "Show resolved"}
        </Button>
      </div>

      {errors.length === 0 ? (
        <p className="mt-5 rounded-lg border border-mint/25 bg-mint/5 p-4 text-sm text-mint">
          Nothing broken.
        </p>
      ) : (
        <div className="mt-4 divide-y divide-ink-800">
          {errors.map((group) => (
            <div key={group.id} className="py-3">
              <div className="flex items-start gap-3">
                <Badge tone={group.resolved ? "neutral" : "amber"}>{group.kind}</Badge>
                <button
                  type="button"
                  className="min-w-0 flex-1 text-left"
                  onClick={() => setExpanded(expanded === group.id ? null : group.id)}
                >
                  <p
                    className={`truncate text-sm ${group.resolved ? "text-ink-500 line-through" : "text-ink-100"}`}
                  >
                    {group.message}
                  </p>
                  <p className="mt-0.5 font-mono text-[11px] text-ink-500">
                    ×{group.occurrences} · last {new Date(group.lastSeen).toLocaleString()} ·{" "}
                    {group.fingerprint.slice(0, 8)}
                  </p>
                </button>
                <Button
                  variant="ghost"
                  size="small"
                  onClick={() => onResolve(group.id, !group.resolved)}
                >
                  {group.resolved ? "Reopen" : "Resolve"}
                </Button>
              </div>
              <AnimatePresence>
                {expanded === group.id && group.detail && (
                  <motion.pre
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="mt-3 overflow-x-auto rounded-lg bg-ink-900 p-3 font-mono text-[11px] leading-relaxed text-ink-400"
                  >
                    {group.detail}
                  </motion.pre>
                )}
              </AnimatePresence>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

function RecentTable({ recent }: { recent: Recent[] }) {
  return (
    <Card className="p-5">
      <h2 className="text-sm font-semibold text-ink-200">Recent projects</h2>
      <div className="mt-3 max-h-[340px] overflow-y-auto">
        {recent.length === 0 && <p className="text-sm text-ink-500">Nothing yet.</p>}
        <table className="w-full text-left text-xs">
          <tbody className="divide-y divide-ink-800">
            {recent.map((row, index) => (
              <tr key={index}>
                <td className="py-2 pr-2">
                  <span
                    className={`inline-block h-1.5 w-1.5 rounded-full ${row.succeeded ? "bg-mint" : "bg-rose"}`}
                  />
                </td>
                <td className="py-2 pr-3">
                  <p className="font-medium text-ink-200">{row.appName}</p>
                  <p className="font-mono text-[11px] text-ink-500">{row.packageName}</p>
                </td>
                <td className="py-2 pr-3 font-mono text-[11px] text-ink-400">
                  {row.features.length} feat
                </td>
                <td className="py-2 pr-3 font-mono text-[11px] text-ink-400">
                  {(row.durationMs / 1000).toFixed(1)}s
                </td>
                <td className="py-2 text-right font-mono text-[11px] text-ink-500">
                  {new Date(row.at).toLocaleTimeString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function HealthTable({ health }: { health: RouteHealth[] }) {
  return (
    <Card className="p-5">
      <h2 className="text-sm font-semibold text-ink-200">Route health</h2>
      <p className="mt-1 text-xs text-ink-500">Last 7 days. Error rate counts 5xx only.</p>
      <table className="mt-3 w-full text-left text-xs">
        <thead>
          <tr className="text-ink-500">
            <th className="pb-2 font-medium">Route</th>
            <th className="pb-2 text-right font-medium">Reqs</th>
            <th className="pb-2 text-right font-medium">5xx</th>
            <th className="pb-2 text-right font-medium">p50</th>
            <th className="pb-2 text-right font-medium">p95</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-800">
          {health.length === 0 && (
            <tr>
              <td colSpan={5} className="py-3 text-ink-500">
                Nothing yet.
              </td>
            </tr>
          )}
          {health.map((row) => (
            <tr key={row.route}>
              <td className="py-2 font-mono text-ink-300">{row.route}</td>
              <td className="py-2 text-right font-mono text-ink-400">{row.requests}</td>
              <td
                className={`py-2 text-right font-mono ${row.errorRate > 0 ? "text-rose" : "text-ink-400"}`}
              >
                {(row.errorRate * 100).toFixed(1)}%
              </td>
              <td className="py-2 text-right font-mono text-ink-400">{row.medianMs}ms</td>
              <td className="py-2 text-right font-mono text-ink-400">{row.p95Ms}ms</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
