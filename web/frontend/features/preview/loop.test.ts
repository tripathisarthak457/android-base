import assert from "node:assert/strict";
import { test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { loop } from "./loop.ts";

/** What framer hands back for an animation that is stopped part-way: a thenable that never settles. */
const stoppedAnimation = { then() {} };

test("a stop ends a wait on an animation that never settles", async () => {
  let runs = 0;
  let ended = false;
  let resumed = false;
  const stop = loop(async ({ settle }) => {
    runs += 1;
    try {
      await settle(stoppedAnimation);
      resumed = true;
    } finally {
      ended = true;
    }
  });
  await sleep(5);
  assert.equal(ended, false);
  stop();
  await sleep(5);
  assert.equal(ended, true);
  assert.equal(resumed, false);
  assert.equal(runs, 1);
});

test("a wait begun after a stop does not wait", async () => {
  const steps: string[] = [];
  const stop = loop(async ({ wait }) => {
    steps.push("press");
    // Not through the run, so a stop cannot end it early.
    await sleep(20);
    steps.push("hold");
    await wait(1);
    steps.push("release");
  });
  await sleep(5);
  stop();
  await sleep(40);
  assert.deepEqual(steps, ["press", "hold"]);
});

test("the body runs again until the loop is stopped", async () => {
  let runs = 0;
  const stop = loop(async ({ wait }) => {
    runs += 1;
    await wait(2);
  });
  await sleep(40);
  stop();
  const seen = runs;
  assert.ok(seen >= 2, `ran ${seen} times`);
  await sleep(20);
  assert.equal(runs, seen);
});

test("a failure in the body is reported, not taken for a stop", async () => {
  const failure = new Error("scope is gone");
  // The test runner fails a test on any unhandled rejection; this one is the point of the test.
  const runner = process.listeners("unhandledRejection");
  process.removeAllListeners("unhandledRejection");
  try {
    const reported = new Promise((resolve) => process.once("unhandledRejection", resolve));
    loop(async ({ wait }) => {
      await wait(1);
      throw failure;
    });
    assert.equal(await reported, failure);
  } finally {
    for (const listener of runner) process.on("unhandledRejection", listener);
  }
});
