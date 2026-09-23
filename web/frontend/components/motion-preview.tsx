"use client";

import { useAnimate, useInView, useReducedMotion } from "framer-motion";
import { useEffect, useRef } from "react";
import { MOTION_TOKENS, type MotionStyleKey, composeSpring } from "../lib/app-theme";

/** A few seconds of the app moving, in one motion style. */
export function MotionPreview({
  styleKey,
  accent,
  onAccent,
  fontName,
}: {
  styleKey: MotionStyleKey;
  accent: string;
  onAccent: string;
  fontName: string;
}) {
  const tokens = MOTION_TOKENS[styleKey];
  const [scope, animate] = useAnimate<HTMLDivElement>();
  const inView = useInView(scope, { margin: "-40px" });
  const reduce = useReducedMotion();
  const pressing = useRef(false);

  const pressSpring = composeSpring(tokens.pressDamping, tokens.pressStiffness);
  const popSpring = composeSpring(1, tokens.pressStiffness * 1.6);
  const navSpring = composeSpring(tokens.navigationDamping, tokens.navigationStiffness);

  async function press() {
    await animate("[data-part=button]", { scale: tokens.pressScale }, pressSpring);
  }

  async function release() {
    if (tokens.pressOvershoot > 1) {
      await animate("[data-part=button]", { scale: tokens.pressOvershoot }, popSpring);
    }
    await animate("[data-part=button]", { scale: 1 }, pressSpring);
  }

  useEffect(() => {
    if (!inView || reduce) return;
    let cancelled = false;
    const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

    async function loop() {
      while (!cancelled) {
        await wait(700);
        if (cancelled) return;
        if (!pressing.current) {
          await press();
          await wait(60);
          await release();
        }

        await wait(450);
        if (cancelled) return;
        // Push: the detail travels the full width, the list beneath a quarter of it and dims.
        animate("[data-part=list]", { x: "-25%", opacity: 0.72 }, {
          x: navSpring,
          opacity: { duration: tokens.medium / 1000, ease: [0.2, 0, 0, 1] },
        });
        await animate("[data-part=detail]", { x: "0%" }, navSpring);

        await wait(900);
        if (cancelled) return;
        animate("[data-part=list]", { x: "0%", opacity: 1 }, {
          x: navSpring,
          opacity: { duration: tokens.medium / 1000, ease: [0.2, 0, 0, 1] },
        });
        await animate("[data-part=detail]", { x: "100%" }, navSpring);

        await wait(600);
        if (cancelled) return;
        // Tab switch: the same fade-through as the shell — out fast, in with a small settle.
        await animate("[data-part=screen]", { opacity: 0 }, { duration: (tokens.instant * 1.5) / 1000, ease: [0.3, 0, 0.8, 0.15] });
        animate("[data-part=tab-a]", { opacity: 0.35 }, { duration: tokens.medium / 1000 });
        animate("[data-part=tab-b]", { opacity: 1 }, { duration: tokens.medium / 1000 });
        await animate(
          "[data-part=screen]",
          { opacity: [0, 1], scale: [0.985, 1] },
          { duration: tokens.medium / 1000, delay: tokens.instant / 1000, ease: [0.05, 0.7, 0.1, 1] },
        );
        await wait(700);
        if (cancelled) return;
        await animate("[data-part=screen]", { opacity: 0 }, { duration: (tokens.instant * 1.5) / 1000 });
        animate("[data-part=tab-a]", { opacity: 1 }, { duration: tokens.medium / 1000 });
        animate("[data-part=tab-b]", { opacity: 0.35 }, { duration: tokens.medium / 1000 });
        await animate(
          "[data-part=screen]",
          { opacity: [0, 1], scale: [0.985, 1] },
          { duration: tokens.medium / 1000, delay: tokens.instant / 1000, ease: [0.05, 0.7, 0.1, 1] },
        );
        await wait(500);
      }
    }

    void loop();
    return () => {
      cancelled = true;
    };
  }, [inView, reduce, styleKey]);

  return (
    <div
      ref={scope}
      className="relative mx-auto h-[210px] w-[118px] overflow-hidden rounded-[18px] border-[3px] border-[#1f1d1a] bg-[#f7f8fa]"
      style={{ fontFamily: `"${fontName}", ui-sans-serif, system-ui, sans-serif` }}
      aria-hidden
    >
      <div data-part="screen" className="absolute inset-x-0 top-0 bottom-[26px] overflow-hidden">
        <div data-part="list" className="absolute inset-0 px-2.5 pt-3">
          <div className="h-2 w-12 rounded-full bg-[#0b0f1a]" />
          <div className="mt-3 space-y-1.5">
            {[0, 1, 2].map((row) => (
              <div key={row} className="rounded-md border border-[#e3e6ec] bg-white p-1.5">
                <div className="h-1.5 w-14 rounded-full bg-[#3d4552]" />
                <div className="mt-1 h-1 w-16 rounded-full bg-[#cbd1db]" />
              </div>
            ))}
          </div>
          <div
            data-part="button"
            onPointerDown={() => {
              pressing.current = true;
              void press();
            }}
            onPointerUp={() => {
              pressing.current = false;
              void release();
            }}
            onPointerLeave={() => {
              if (pressing.current) {
                pressing.current = false;
                void release();
              }
            }}
            className="mt-3 flex h-6 cursor-pointer select-none items-center justify-center rounded-md text-[8px] font-bold"
            style={{ background: accent, color: onAccent }}
          >
            Press me
          </div>
        </div>
        <div
          data-part="detail"
          className="absolute inset-0 bg-white px-2.5 pt-3 shadow-[-6px_0_12px_rgba(11,15,26,0.08)]"
          style={{ transform: "translateX(100%)" }}
        >
          <div className="h-2 w-16 rounded-full bg-[#0b0f1a]" />
          <div className="mt-2 space-y-1">
            {[0, 1, 2, 3].map((line) => (
              <div key={line} className="h-1 rounded-full bg-[#cbd1db]" style={{ width: `${88 - line * 14}%` }} />
            ))}
          </div>
          <div className="mt-3 h-12 rounded-md" style={{ background: accent, opacity: 0.18 }} />
        </div>
      </div>
      <div className="absolute inset-x-0 bottom-0 flex h-[26px] items-center justify-around border-t border-[#e9ecf1] bg-white">
        <span data-part="tab-a" className="h-2 w-2 rounded-full" style={{ background: accent }} />
        <span data-part="tab-b" className="h-2 w-2 rounded-full opacity-35" style={{ background: accent }} />
        <span className="h-2 w-2 rounded-full bg-[#cbd1db]" />
      </div>
    </div>
  );
}
