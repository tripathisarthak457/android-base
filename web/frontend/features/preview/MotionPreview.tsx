"use client";

import { useAnimate, useInView } from "framer-motion";
import { useEffect, useRef } from "react";
import type { Tokens } from "../catalogue/types.ts";
import { SHELL_SPRINGS, TAB_REVEAL_SHIFT, barLook } from "./bar.ts";
import { loop } from "./loop.ts";
import { type PreviewTheme, framerSpring } from "./theme.ts";

/** Where each moving part sits between runs, as the markup first draws it. */
const REST: [part: string, values: Record<string, string | number>][] = [
  ["button", { scale: 1 }],
  ["list", { x: "0%" }],
  ["scrim", { opacity: 0 }],
  ["detail", { x: "100%" }],
  ["page", { opacity: 1 }],
  ["other", { opacity: 0, x: "0%" }],
  ["indicator", { x: "0%" }],
];

/** The tab reveal's offset as a share of the screen, which the template draws 360 dp wide. */
const REVEAL_SHIFT = `${(TAB_REVEAL_SHIFT / 360) * 100}%`;

/**
 * A few seconds of the app moving in one motion style: a press and release on the style's own
 * springs, then a push and pop (slide only, the covered screen darkened by a drawn scrim and
 * moving at the parallax fraction) and a tab switch (the arriving tab nudged in from the side it
 * comes from, the indicator on its leading-edge spring), on the springs the template's shell uses
 * in every style. It loops while in view and not `paused`; the button answers a press either way.
 */
export function MotionPreview({ theme, tokens, font, paused }: { theme: PreviewTheme; tokens: Tokens; font: string; paused: boolean }) {
  const [scope, animate] = useAnimate<HTMLDivElement>();
  const inView = useInView(scope, { margin: "-40px" });
  const pressing = useRef(false);
  const { press, navigation } = tokens.motion;
  const scrim = theme.dark ? navigation.scrimAlpha.dark : navigation.scrimAlpha.light;

  const down = () => animate("[data-part=button]", { scale: press.scale }, theme.spring("press"));
  const up = () => animate("[data-part=button]", { scale: 1 }, theme.spring("release"));

  useEffect(() => {
    // A run stopped part-way, by scrolling off, a pause or a theme change, leaves the detail
    // half-pushed or the other tab showing, and React does not restore styles it never changed.
    for (const [part, values] of REST) animate(`[data-part=${part}]`, values, { duration: 0 });
    if (!inView || paused) return;
    const screen = framerSpring(SHELL_SPRINGS.screen);
    const selection = framerSpring(SHELL_SPRINGS.indicatorLead);
    const reveal = framerSpring(SHELL_SPRINGS.tabReveal);

    return loop(async ({ settle, wait }) => {
      await wait(700);
      // The button's springs are timed, not awaited: a press on it starts animations that stop the
      // loop's, and framer never settles a stopped animation. A visitor's press is never released
      // by the loop.
      if (!pressing.current) {
        void down();
        await wait(240);
        if (!pressing.current) void up();
      }
      await wait(770);
      animate("[data-part=list]", { x: `${-navigation.parallax * 100}%` }, screen);
      animate("[data-part=scrim]", { opacity: scrim }, screen);
      await settle(animate("[data-part=detail]", { x: "0%" }, screen));
      await wait(900);
      animate("[data-part=list]", { x: "0%" }, screen);
      animate("[data-part=scrim]", { opacity: 0 }, screen);
      await settle(animate("[data-part=detail]", { x: "100%" }, screen));
      await wait(600);
      animate("[data-part=page]", { opacity: 0 }, { duration: 0 });
      animate("[data-part=other]", { opacity: 1 }, { duration: 0 });
      animate("[data-part=other]", { x: [REVEAL_SHIFT, "0%"] }, reveal);
      await settle(animate("[data-part=indicator]", { x: "100%" }, selection));
      await wait(900);
      animate("[data-part=other]", { opacity: 0 }, { duration: 0 });
      animate("[data-part=page]", { opacity: 1 }, { duration: 0 });
      // The list tab arrives whole, its button with it; the other tab inside it is hidden by now.
      animate("[data-part=list]", { x: [`-${REVEAL_SHIFT}`, "0%"] }, reveal);
      await settle(animate("[data-part=indicator]", { x: "0%" }, selection));
      await wait(500);
    });
    // `down`, `up` and the token values all follow from `theme`, so the loop restarts with it.
  }, [inView, paused, theme]);

  const surface = theme.colour("surface");
  const line = theme.colour("outline");
  const bar = barLook(theme.style, theme.colour, theme.dark);
  return (
    <div
      ref={scope}
      className="relative mx-auto h-[210px] w-[118px] shrink-0 overflow-hidden rounded-[18px] border-[3px] border-[#1f1d1a]"
      style={{ fontFamily: `"${font}", ui-sans-serif, system-ui, sans-serif`, background: theme.colour("background") }}
      aria-hidden
    >
      <div className="absolute inset-x-0 top-0 bottom-[30px] overflow-hidden">
        <div data-part="list" className="absolute inset-0 px-2.5 pt-3" style={{ background: theme.colour("background") }}>
          <div data-part="page">
            <div className="h-2 w-12 rounded-full" style={{ background: theme.colour("contentPrimary") }} />
            <div className="mt-3 space-y-1.5">
              {[0, 1, 2].map((row) => (
                <div key={row} className="rounded-md p-1.5" style={{ background: surface, border: `1px solid ${theme.colour("separator")}` }}>
                  <div className="h-1.5 w-14 rounded-full" style={{ background: theme.colour("contentSecondary") }} />
                  <div className="mt-1 h-1 w-16 rounded-full" style={{ background: line }} />
                </div>
              ))}
            </div>
          </div>
          <div
            data-part="button"
            onPointerDown={() => {
              pressing.current = true;
              void down();
            }}
            onPointerUp={() => {
              pressing.current = false;
              void up();
            }}
            onPointerLeave={() => {
              if (pressing.current) {
                pressing.current = false;
                void up();
              }
            }}
            // The phone sits in a motion style's card, which is a label: feeling a style's press
            // must not also choose it.
            onClick={(event) => event.preventDefault()}
            className="mt-3 flex h-6 cursor-pointer select-none items-center justify-center rounded-md text-[8px] font-bold"
            style={{ background: theme.colour("accent"), color: theme.colour("onAccent") }}
          >
            Press me
          </div>
          <div data-part="other" className="absolute inset-0 px-2.5 pt-3" style={{ opacity: 0, background: theme.colour("background") }}>
            <div className="h-2 w-10 rounded-full" style={{ background: theme.colour("contentPrimary") }} />
            <div className="mt-3 grid grid-cols-2 gap-1.5">
              {[0, 1, 2, 3].map((cell) => (
                <div key={cell} className="h-12 rounded-md" style={{ background: surface, border: `1px solid ${theme.colour("separator")}` }} />
              ))}
            </div>
          </div>
          <div data-part="scrim" className="pointer-events-none absolute inset-0 bg-black" style={{ opacity: 0 }} />
        </div>
        <div data-part="detail" className="absolute inset-0 px-2.5 pt-3" style={{ transform: "translateX(100%)", background: theme.colour("background") }}>
          <div className="pointer-events-none absolute inset-y-0 -left-2 w-2 bg-gradient-to-l from-black/15 to-transparent" />
          <div className="h-2 w-16 rounded-full" style={{ background: theme.colour("contentPrimary") }} />
          <div className="mt-2 space-y-1">
            {[0, 1, 2, 3].map((row) => (
              <div key={row} className="h-1 rounded-full" style={{ width: `${88 - row * 14}%`, background: line }} />
            ))}
          </div>
          <div className="mt-3 h-12 rounded-md" style={{ background: theme.colour("accentSubtle") }} />
        </div>
      </div>
      <div
        className="absolute inset-x-1.5 bottom-1.5 flex h-[22px] items-center rounded-full px-0.5"
        style={{ background: bar.surface, border: `1px solid ${bar.border}` }}
      >
        {[0, 1, 2].map((tab) => (
          <span key={tab} className="flex flex-1 justify-center">
            <span className="h-2 w-2 rounded-full" style={{ background: bar.content }} />
          </span>
        ))}
        {/* The selected tab's dot rides inside the pill, in the colour the template draws there. */}
        <span
          data-part="indicator"
          className="absolute inset-y-[3px] left-0.5 flex w-[calc((100%-4px)/3)] items-center justify-center rounded-full"
          style={{ background: bar.indicator }}
        >
          <span className="h-2 w-2 rounded-full" style={{ background: bar.selectedContent }} />
        </span>
      </div>
    </div>
  );
}
