// A looping preview's timeline: an async body run over and over, stoppable at any of its waits.

/** Anything a run can wait on: a promise, or a framer animation, which is a thenable. */
type Thenable = { then: (onResolve: () => void, onReject?: (reason?: unknown) => void) => unknown };

export type Run = {
  /** Waits until `step` settles. */
  settle: (step: Thenable) => Promise<void>;
  wait: (ms: number) => Promise<void>;
};

const STOPPED = Symbol("stopped");

/**
 * Runs `body` again and again until the returned function is called. Stopping ends the wait the
 * body is in at once, rather than when it settles: framer never settles an animation stopped
 * part-way, as a pause, a theme change or an unmount stops it, so a body waiting on one would
 * wait forever. Nothing after that wait runs, so a body never animates a scope that has gone.
 * Any other failure is rethrown, to reach the page's unhandled rejection report.
 */
export function loop(body: (run: Run) => Promise<void>): () => void {
  let stopped = false;
  let interrupt = () => {};
  // `begin` starts a wait and returns how to cancel it. The body awaits one at a time, so the
  // latest is the only one a stop has to end.
  const until = (begin: (resolve: () => void, reject: (reason: unknown) => void) => () => void) =>
    new Promise<void>((resolve, reject) => {
      if (stopped) {
        reject(STOPPED);
        return;
      }
      const cancel = begin(() => resolve(), reject);
      interrupt = () => {
        cancel();
        reject(STOPPED);
      };
    });
  const run: Run = {
    settle: (step) =>
      until((resolve, reject) => {
        step.then(resolve, reject);
        return () => {};
      }),
    wait: (ms) =>
      until((resolve) => {
        const timer = setTimeout(resolve, ms);
        return () => clearTimeout(timer);
      }),
  };
  (async () => {
    while (!stopped) await body(run);
  })().catch((error: unknown) => {
    if (error !== STOPPED) throw error;
  });
  return () => {
    stopped = true;
    interrupt();
  };
}
