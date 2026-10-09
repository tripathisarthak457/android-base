"use client";

import { useAnimate, useInView, useReducedMotion } from "framer-motion";
import { useEffect, useRef } from "react";
import type { Tokens } from "../catalogue/types.ts";
import type { PreviewTheme } from "./theme.ts";

/**
 * A few seconds of the app moving in one motion style, on its own springs: a press and release,
 * a push and pop (slide only, the covered screen darkened by a drawn scrim and moving at the
 * parallax fraction), and a tab switch (instant content, the indicator on the selection spring).
 */
export function MotionPreview({ theme, tokens, font }: { theme: PreviewTheme; tokens: Tokens; font: string }) {
  const [scope, animate] = useAnimate<HTMLDivElement>();
  const inView = useInView(scope, { margin: "-40px" });
  const reduce = useReducedMotion();
  const pressing = useRef(false);
  const { press, navigation } = tokens.motion;
  const scrim = theme.dark ? navigation.scrimAlpha.dark : navigation.scrimAlpha.light;

  const down = () => animate("[data-part=button]", { scale: press.scale }, theme.spring("press"));
  const up = () => animate("[data-part=button]", { scale: 1 }, theme.spring("release"));

  useEffect(() => {
    if (!inView || reduce) return;
    let cancelled = false;
    const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
    const screen = theme.spring("screen");
    const selection = theme.spring("selection");

    async function loop() {
      while (!cancelled) {
        await wait(700);
        if (cancelled) return;
        if (!pressing.current) {
          await down();
          await wait(80);
          await up();
        }
        await wait(450);
        if (cancelled) return;
        animate("[data-part=list]", { x: `${-navigation.parallax * 100}%` }, screen);
        animate("[data-part=scrim]", { opacity: scrim }, screen);
        await animate("[data-part=detail]", { x: "0%" }, screen);
        await wait(900);
        if (cancelled) return;
        animate("[data-part=list]", { x: "0%" }, screen);
        animate("[data-part=scrim]", { opacity: 0 }, screen);
        await animate("[data-part=detail]", { x: "100%" }, screen);
        await wait(600);
        if (cancelled) return;
        animate("[data-part=page]", { opacity: 0 }, { duration: 0 });
        animate("[data-part=other]", { opacity: 1 }, { duration: 0 });
        await animate("[data-part=indicator]", { x: "100%" }, selection);
        await wait(900);
        if (cancelled) return;
        animate("[data-part=other]", { opacity: 0 }, { duration: 0 });
        animate("[data-part=page]", { opacity: 1 }, { duration: 0 });
        await animate("[data-part=indicator]", { x: "0%" }, selection);
        await wait(500);
      }
    }
    void loop();
    return () => {
      cancelled = true;
    };
    // `down`, `up` and the token values all follow from `theme`, so the loop restarts with it.
  }, [inView, reduce, theme]);

  const surface = theme.colour("surface");
  const line = theme.colour("borderStrong");
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
                <div key={row} className="rounded-md p-1.5" style={{ background: surface, border: `1px solid ${theme.colour("border")}` }}>
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
            className="mt-3 flex h-6 cursor-pointer select-none items-center justify-center rounded-md text-[8px] font-bold"
            style={{ background: theme.colour("accent"), color: theme.colour("onAccent") }}
          >
            Press me
          </div>
          <div data-part="other" className="absolute inset-0 px-2.5 pt-3" style={{ opacity: 0, background: theme.colour("background") }}>
            <div className="h-2 w-10 rounded-full" style={{ background: theme.colour("contentPrimary") }} />
            <div className="mt-3 grid grid-cols-2 gap-1.5">
              {[0, 1, 2, 3].map((cell) => (
                <div key={cell} className="h-12 rounded-md" style={{ background: surface, border: `1px solid ${theme.colour("border")}` }} />
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
      <div className="absolute inset-x-1.5 bottom-1.5 flex h-[22px] items-center rounded-full px-0.5" style={{ background: surface, border: `1px solid ${theme.colour("separator")}` }}>
        <span data-part="indicator" className="absolute inset-y-[3px] left-0.5 w-[calc((100%-4px)/3)] rounded-full" style={{ background: theme.colour("accentSubtle") }} />
        {[0, 1, 2].map((tab) => (
          <span key={tab} className="relative z-10 flex flex-1 justify-center">
            <span className="h-2 w-2 rounded-full" style={{ background: tab === 2 ? line : theme.colour("accentContent") }} />
          </span>
        ))}
      </div>
    </div>
  );
}
