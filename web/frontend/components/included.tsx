import { COMING_WITH_RELEASE, isSetupOnly } from "../features/catalogue/options.ts";
import { includedItems } from "../features/landing/included.ts";
import { CATALOGUE } from "../lib/catalogue.ts";
import { Badge } from "./ui.tsx";

export function Included() {
  const setupOnly = isSetupOnly(CATALOGUE);
  return (
    <section className="border-y border-ink-800 bg-ink-900/40">
      <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
        <div className="max-w-2xl">
          <h2 className="font-display text-4xl font-bold text-ink-100">This part is not optional</h2>
          <p className="mt-3 text-ink-300">
            Everything below arrives whatever you tick. It is the reason the template exists, and the part that would take a
            fortnight to assemble by hand.
            {setupOnly && " Until the Android release this site makes setup-only projects, so what needs UI components is marked."}
          </p>
        </div>

        <ul className="mt-10 grid gap-4 md:grid-cols-2">
          {includedItems(CATALOGUE).map((item) => (
            <li key={item.title} className="rounded-xl border border-ink-700 bg-ink-850 p-5">
              <div className="flex items-start justify-between gap-4">
                <h3 className="font-semibold text-ink-100">{item.title}</h3>
                <code className="shrink-0 rounded bg-ink-800 px-2 py-0.5 font-mono text-[11px] text-ink-300">{item.detail}</code>
              </div>
              {setupOnly && item.components && (
                <p className="mt-2">
                  <Badge tone="amber">{COMING_WITH_RELEASE}</Badge>
                </p>
              )}
              <p className="mt-2.5 text-sm leading-relaxed text-ink-300">{item.body}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
