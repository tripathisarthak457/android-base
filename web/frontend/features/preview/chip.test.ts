import assert from "node:assert/strict";
import { test } from "node:test";
import { loadCatalogue, readText } from "../catalogue/testing.ts";
import { CHIP, chipRoles } from "./chip.ts";

const source = readText(
  ..."templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/selection/AppChip.kt".split("/"),
);

test("the chip is filled with no outline, and takes the accent's tint when selected, as AppChip is", () => {
  assert.doesNotMatch(source, /\.border\(/, "AppChip draws an outline now; the preview's chip needs one too");
  const container = /val container = if \(selected\) colors\.(\w+) else colors\.(\w+)/.exec(source);
  const content = /val content = if \(selected\) colors\.(\w+) else colors\.(\w+)/.exec(source);
  assert.deepEqual(chipRoles(true), { container: container?.[1], content: content?.[1] });
  assert.deepEqual(chipRoles(false), { container: container?.[2], content: content?.[2] });
});

test("the chip's sizes and type are AppChip's", () => {
  const dp = (name: string) => Number(new RegExp(`val ${name} = ([\\d.]+)\\.dp`).exec(source)?.[1]);
  assert.equal(CHIP.height, dp("ChipHeight"));
  assert.equal(CHIP.remove, dp("RemoveGlyph"));
  assert.equal(CHIP.paddingEndRemovable, dp("RemoveEndPadding"));
  assert.match(source, new RegExp(`padding\\(start = ${CHIP.padding}\\.dp, end = if \\(onRemove != null\\) RemoveEndPadding else ${CHIP.padding}\\.dp\\)`));
  assert.match(source, new RegExp(`Arrangement\\.spacedBy\\(${CHIP.gap}\\.dp\\)`));
  assert.match(source, new RegExp(`style = AppTheme\\.typography\\.${CHIP.text},`));
  assert.match(source, /size = AppTheme\.sizes\.iconSmall\)/);
  assert.equal(CHIP.icon, loadCatalogue().tokens.sizes.iconSmall);
});
