/** What the configurator has answered so far, as a bug report attaches it. */
export type ReportContext = {
  target: string;
  appName: string;
  packageName: string;
  features: string[];
  preset: string;
  minSdk: number;
  motionStyle: string;
  fontName: string;
  accentColour: string;
};

let source: (() => ReportContext) | null = null;

/**
 * The configurator registers a reader here, and the report button in the navigation calls it
 * when the dialog opens, so neither re-renders the other as answers change.
 */
export function provideReportContext(read: () => ReportContext): () => void {
  source = read;
  return () => {
    if (source === read) source = null;
  };
}

export function readReportContext(): ReportContext | null {
  return source?.() ?? null;
}
