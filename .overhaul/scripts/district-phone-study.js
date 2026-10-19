export const meta = {
  name: 'district-phone-study',
  description: "Measure District by Zomato against the template on the user's Redmi (sheets, transitions, feed, back gesture): a Sonnet operator records and times the same scenes on both apps, an Opus analyst fits the curves and frame numbers and says whether the template is already ahead, a separate Opus reviewer checks it",
  phases: [
    { title: 'Capture', detail: 'a Sonnet operator runs research/district/PHONE-STUDY.md on District and on the newest template build' },
    { title: 'Analyse', detail: 'Opus fits the motion, compares frame numbers, decides which proposals survive' },
    { title: 'Review', detail: 'a separate Opus reviewer checks the numbers against the raw data, then one revision' },
  ],
}

// The user, Oct 19 20:27 and 20:30: District is installed and signed in on the Redmi, "except making orders and doing any
// payment or booking u can use it freely the app to understand the animations and all"; "i feel the app is smooth on my
// poco f7 only in my redmi it was lagging so i guess we ahead already ... but pls confirm in next run". REQUIREMENTS 45.
// Their broad permission answers PHONE-STUDY.md §2.1 question 2 (reading window and view class names at runtime); for
// question 1 the run studies the sheets it can reach safely and lists them, and the user can name favourites later.
// Model routing (global CLAUDE.md §10): driving the phone and recording is a fixed procedure, so a Sonnet operator does
// it and returns facts with paths; fitting, comparing and every verdict is Opus, then a separate Opus reviewer.
const OV = 'D:/projects/tools/.overhaul'
const RD = `${OV}/research/district`
const L = `${OV}/bin/locked`

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) first. adb only through ${L} adb, ffmpeg and Python through ${OV}/bin/memwait; long commands in the background with logs under ${RD}/phone/logs/, polled. Phone 9a7c74f4: recordings and dumps to /data/local/tmp, never /sdcard; screenrecord --size 720x1600; restore any device setting you change. District (com.application.zomato.district) belongs to the user and is signed in to their account: never order, pay, book, add to a cart, pick seats, change the account, its settings or permissions, or open the profile, tickets or payment screens; never uninstall, clear or force-compile it; every tap goes through ${RD}/tools/safe_target.py exactly as ${RD}/PHONE-STUDY.md §1 says; obey §9's stop conditions (with the user away, a refused tap means skip that scene and record why). Everything under ${RD}/phone/ stays on this machine (it can show the user's account); never commit or push anything.`

const RAW = { type: 'object', properties: {
  runs: { type: 'array', items: { type: 'object', properties: { scene: { type: 'string' }, app: { type: 'string' }, command: { type: 'string' }, exit_code: { type: 'integer' }, outputs: { type: 'string' }, note: { type: 'string' } }, required: ['scene', 'app', 'command', 'exit_code', 'outputs'] } },
  skipped: { type: 'array', items: { type: 'string' } },
  device: { type: 'string' },
  problems: { type: 'array', items: { type: 'string' } },
}, required: ['runs', 'skipped', 'device', 'problems'] }
const VERDICT = { type: 'object', properties: {
  ahead: { type: 'string', enum: ['template-ahead', 'district-ahead', 'mixed', 'inconclusive'] },
  summary: { type: 'string' },
  scenes: { type: 'array', items: { type: 'object', properties: { scene: { type: 'string' }, district: { type: 'string' }, template: { type: 'string' }, better: { type: 'string' } }, required: ['scene', 'district', 'template', 'better'] } },
  proposals: { type: 'array', items: { type: 'object', properties: { id: { type: 'string' }, keep: { type: 'string', enum: ['keep', 'change', 'drop'] }, why: { type: 'string' } }, required: ['id', 'keep', 'why'] } },
  results_path: { type: 'string' },
}, required: ['ahead', 'summary', 'scenes', 'proposals', 'results_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['where', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Capture')
const raw = await agent(`You are a careful operator. Follow ${RD}/PHONE-STUDY.md exactly and report facts only: what you ran, the exit code, the files you produced. Do not judge what the recordings show, do not edit code, do not commit. If a step fails, record it and go on. ${RULES}
Run §2.2, §2.3 (record District's versionName and both apps' compilation filters with dumpsys package, read-only), §3, §4.0 to §4.4 on District, §5 on the template, and §6 (the user allowed it). The template build: the newest combined build the bug fix installed (pm list packages | grep com.example.bug.combined; ${OV}/bugs/2026-10-19-back-licenses/CAPTURE-PROCEDURE.md says how it is built and installed); if its compilation filter is not speed-profile, run cmd package compile -m speed-profile -f on that test package only, so both apps are measured compiled, and record it. For the sheets, study every District sheet you can open through a safe tap (filters, sort, location or language choosers and the like; none inside a booking flow), list them, and use the same sheet types on the template. Then pull everything to ${RD}/phone/ and leave District and the phone as you found them (Home screen).`, { label: 'district:capture', phase: 'Capture', schema: RAW, model: 'sonnet' })
if (!raw) throw new Error('capture failed')

phase('Analyse')
const ANALYSE = (extra) => `You are the Opus motion and performance analyst of android-base. A Sonnet operator recorded District by Zomato and the template on the user's Redmi following ${RD}/PHONE-STUDY.md; trust nothing until you check it. Its report: ${JSON.stringify(raw)}. ${RULES} Do not touch the phone except to re-take a capture that is missing or wrong, under the same rules.
The user's question (Oct 19 20:30): District feels smooth on their Poco F7 but lags on this Redmi, so is the template already ahead here? Answer it with numbers.
1. Measure per ${RD}/PHONE-STUDY.md §7 with the tools in ${RD}/tools/: track and fit every sheet, transition and scroll scene (t50/t90/t99, overshoot, spring or tween, velocity carried), and per-window frame numbers (janky %, p90/p99 frame time, hitches, first and opening frames, tap-to-present). Open the recordings' frames yourself where a fit looks wrong.
2. Compare scene by scene, District beside the template, and say which is better on this phone and why; note the compilation filters and anything that makes a comparison unfair.
3. Answer every row of ${RD}/RESEARCH.md §2.6 and decide for each of P1 to P10 keep, change or drop, with the numbers; drop anything the template already does better.
4. Write ${RD}/PHONE-RESULTS.md as §8 says (numbers and verdicts, no images), with a plain-English part first for the user: is the template ahead on the Redmi, where District is still better, and which proposals are worth their approval.${extra}`
let v = await agent(ANALYSE(''), { label: 'district:analyse', phase: 'Analyse', schema: VERDICT })
if (!v) throw new Error('analysis failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer. Check ${RD}/PHONE-RESULTS.md against the raw data in ${RD}/phone/ (re-run the fits and frame summaries for the scenes that decide the verdict, open frames where needed), against ${RD}/RESEARCH.md and against the rules. ${RULES} Do not touch the phone and do not modify anything.
The analyst's summary: ${JSON.stringify(v)}
Blocking means: a number the raw data does not give, a verdict the numbers do not support, an unfair comparison not stated (compilation state, different scenes, a window missed), a proposal kept that the template already does better or dropped that the numbers support, or any sign the run broke a rule (a tap outside safe_target.py, a booking or account screen opened, phone/ content committed). There is no pressure to find something.`, { label: 'district:review', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(ANALYSE(`\nThis is a revision. A separate reviewer found these blocking problems: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in PHONE-RESULTS.md and return the full updated result.`), { label: 'district:revise', phase: 'Review', schema: VERDICT })
  if (revised) v = revised
}
return { verdict: v, review }
