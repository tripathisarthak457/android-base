"use client";

import { MotionConfig } from "framer-motion";
import type { ReactNode } from "react";

/** framer-motion's springs follow the visitor's reduced-motion setting, as the CSS ones do. */
export function SiteMotion({ children }: { children: ReactNode }) {
  return <MotionConfig reducedMotion="user">{children}</MotionConfig>;
}
