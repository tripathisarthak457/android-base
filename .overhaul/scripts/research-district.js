export const meta = {
  name: 'research-district',
  description: "Research why District by Zomato feels so fluid (bottom sheet, transitions, components) and what android-base could take from it: public sources first, then a gap analysis against the template and a list of proposals for the user to approve one by one. No code changes; the phone study comes later",
  phases: [
    { title: 'Gather', detail: 'three Sonnet lookups: engineering sources, design and video sources, technique references' },
    { title: 'Verify', detail: 'an Opus verifier checks every claim against its source' },
    { title: 'Compare', detail: 'Opus gap analysis against the template, proposals, the phone study procedure' },
    { title: 'Review', detail: 'a separate Opus reviewer checks claims against evidence, then one revision' },
  ],
}

// The user, Oct 19 17:00: "the animations fluidity of one app that i use is so great ... District by Zomato ... I like its
// bottom sheet and other components ... what we can incorporate ... Every change suggested from that app in my project
// should go through my approval and i wanna know what they are doing different ... u can even install that app on my
// device after ur research". REQUIREMENTS 45. This run changes no code and does not use the phone (the bug fix has it);
// the phone study is a later run with the user's install and sign-in.
// Model routing (global CLAUDE.md §10): web lookups are mechanical, so Sonnet gathers facts with sources; the verifier,
// the analysis and the review are Opus.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const RD = `${OV}/research/district`

const RULES = `Rules: read-only everywhere except your own files under ${RD}/. Never run gradle or adb, never touch a worktree under ${OV}/wt/ (other runs are editing them), never commit or push. Read code from branch heads with git -C ${R} show <branch>:<path> or git -C ${R} grep -n <pattern> <branch> -- templates/android. Respect the app's owners: use only public sources and what the app visibly does; never download its APK from a mirror site, never decompile, extract or copy its code or assets. Copyright: summarise in your own words, quote at most a few words with the source.`
const LOOKUP = `You are a careful researcher doing lookups. Report facts only, each with its source URL and the date it was published if shown; say plainly when a claim is a guess, an opinion, or from an unofficial source. Do not judge what android-base should do. Use WebSearch (standard mode first, extended only for thin or hard-to-find results) and WebFetch. Save your notes to the file named below as you go.`

const FOUND = { type: 'object', properties: {
  findings: { type: 'array', items: { type: 'object', properties: {
    topic: { type: 'string' }, claim: { type: 'string' }, source: { type: 'string' }, date: { type: 'string' },
    kind: { type: 'string', enum: ['official', 'engineer', 'third-party', 'video', 'opinion', 'guess'] },
  }, required: ['topic', 'claim', 'source', 'kind'] } },
  not_found: { type: 'array', items: { type: 'string' } },
  notes_path: { type: 'string' },
}, required: ['findings', 'not_found', 'notes_path'] }
const VERIFIED = { type: 'object', properties: {
  facts: { type: 'array', items: { type: 'object', properties: { claim: { type: 'string' }, source: { type: 'string' }, confidence: { type: 'string', enum: ['confirmed', 'likely', 'unverified'] }, why: { type: 'string' } }, required: ['claim', 'source', 'confidence', 'why'] } },
  dropped: { type: 'array', items: { type: 'string' } },
  stack: { type: 'string' },
  report_path: { type: 'string' },
}, required: ['facts', 'dropped', 'stack', 'report_path'] }
const PROPOSED = { type: 'object', properties: {
  summary: { type: 'string' },
  differences: { type: 'array', items: { type: 'string' } },
  proposals: { type: 'array', items: { type: 'object', properties: {
    id: { type: 'string' }, title: { type: 'string' }, district: { type: 'string' }, ours: { type: 'string' },
    change: { type: 'string' }, owner: { type: 'string' }, redmi_cost: { type: 'string' }, effort: { type: 'string' },
    risk: { type: 'string' }, evidence: { type: 'string' }, needs_phone_check: { type: 'boolean' },
  }, required: ['id', 'title', 'district', 'ours', 'change', 'owner', 'redmi_cost', 'effort', 'risk', 'evidence', 'needs_phone_check'] } },
  already_ours: { type: 'array', items: { type: 'string' } },
  phone_study_path: { type: 'string' },
  report_path: { type: 'string' },
}, required: ['summary', 'differences', 'proposals', 'already_ours', 'phone_study_path', 'report_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['where', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Gather')
const [eng, design, tech] = await parallel([
  () => agent(`${LOOKUP} ${RULES} Notes file: ${RD}/gather-engineering.md.
Find what is public about how District by Zomato (the going-out app from Zomato / Eternal Ltd: movies, events, dining) is built, Android first: its Play Store listing (package id, size, last update, minimum Android version, ratings, what reviews say about speed and smoothness); the UI toolkit (Jetpack Compose, Android Views, React Native, Flutter, or a mix) from Zomato or Eternal engineering blog posts, conference talks (Droidcon India and others), engineers' LinkedIn or X posts, job postings for the District Android or mobile team, open-source repositories under github.com/Zomato; anything published about District's or Zomato's animation, bottom sheet, motion system, server-driven UI, image loading, performance work or design system (Sushi is Zomato's design system: find what is public about it).`, { label: 'gather:engineering', phase: 'Gather', schema: FOUND, model: 'sonnet' }),
  () => agent(`${LOOKUP} ${RULES} Notes file: ${RD}/gather-design.md.
Find design and visual sources on District by Zomato's interface and motion: its designers' case studies (Behance, Dribbble, Medium, personal sites), the design team's posts, app teardown articles, UI galleries (Mobbin and similar), award pages, and YouTube or social videos that show the app in use. For each video, note the timestamps that show the bottom sheet, screen transitions, the home feed, ticket or seat selection, scrolling headers and any notable micro-interaction, and describe in words what is seen (direction, rough duration, overshoot or settle, blur, scale, what follows the finger). Note what users and designers praise about its feel.`, { label: 'gather:design', phase: 'Gather', schema: FOUND, model: 'sonnet' }),
  () => agent(`${LOOKUP} ${RULES} Notes file: ${RD}/gather-technique.md.
Collect current references (prefer 2025-2026, official first) on how the most fluid Android apps build these, in Jetpack Compose and in Views: a bottom sheet that follows the finger and settles with a spring that keeps the release velocity, the hand-off between the sheet's drag and its inner list's scroll (nested scrolling), detents, the dimmed or blurred backdrop, predictive back on sheets; shared-element and container transforms between a card and its detail; collapsing and sticky headers driven by scroll; interruptible, velocity-preserving animations; Material 3 Expressive's spring-based motion scheme; and the costs of each on a low-end phone (blur, large alpha layers, recomposition per frame). Android developer docs, androidx release notes, Google I/O and Android Dev Summit talks, and well-known engineering blogs.`, { label: 'gather:technique', phase: 'Gather', schema: FOUND, model: 'sonnet' }),
])
const gathered = [eng, design, tech].filter(Boolean)
if (!gathered.length) throw new Error('every lookup failed')
if (gathered.length < 3) log(`${3 - gathered.length} lookup(s) failed; verifying what came back`)

phase('Verify')
const verified = await agent(`You are the Opus verifier of three Sonnet lookups about District by Zomato's motion and how it is built. Trust nothing until you check it. ${RULES}
Their findings: ${JSON.stringify(gathered)}
Open the sources for every claim that matters (the UI toolkit, anything about the bottom sheet or motion, every number) and mark each confirmed, likely or unverified with why; drop what the source does not say. Watch the key videos' marked timestamps where you can, and keep descriptions of motion as observations, not as facts about the code. State the most likely UI stack with its evidence. Write ${RD}/SOURCES.md: the verified facts grouped by topic, each with its source, and what could not be found.`, { label: 'verify:sources', phase: 'Verify', schema: VERIFIED })
if (!verified) throw new Error('verifier failed')

phase('Compare')
const COMPARE = (extra) => `You are the motion and design-system lead of android-base. ${RULES}
The user loves how fluid District by Zomato feels, especially its bottom sheet, and wants to know what District does differently and what the template could take from it; every change you suggest needs their approval one by one, so you propose and implement nothing. The template's goal (the user, Oct 19): light and smooth, the most optimised visual feel, judged on a budget Redmi Note 10 Pro Max (Snapdragon 732G, 1080x2400). Verified sources: ${JSON.stringify(verified)}.
Read the template's motion work first: ${OV}/research/MOTION-PLAN.md, ${OV}/research/SHEET-SPEC.md and ${OV}/research/sheet/, ${OV}/research/SMOOTHNESS-PLAN.md, ${OV}/research/ios-motion.md, ${OV}/REQUIREMENTS.md (19, 21, 22, 24, 28, 29, 42), and the code on the branch heads: wp/WP03 (design system: AppMotion springs, AppSheet, AppOverscroll, components), wp/WP04 (navigation shell, back card, transitions), wp/WP05-glass (glass and blur).
Then write ${RD}/RESEARCH.md, plain English for the user first and engineering detail after: what District does differently (separating what the sources confirm from what is observed in videos and what is a guess), what android-base already does as well or better, and the proposals. Each proposal: what District does, what we do today (file:line), the change, the owning package, the cost on the Redmi (frames, memory, overdraw, blur), effort, risk, the evidence, and whether it needs the phone study to confirm. Order them by how much the user would feel them. Leave out anything that would cost smoothness on the Redmi, and say why.
Also write ${RD}/PHONE-STUDY.md, a procedure for a later run on the user's phone once the user has installed District from the Play Store and signed in themselves: which screens and gestures to record (bottom sheet open, drag, fling, cancel and dismiss, its inner scroll; screen transitions; the home feed; headers; back gesture), screenrecord settings (--size 720x1600, /data/local/tmp), dumpsys gfxinfo framestats for frame times, frame-by-frame extraction with ffmpeg to measure the sheet's position per frame and fit its curve (duration, overshoot, damping, whether release velocity is kept), the same scenes on the template's catalog for a side-by-side, and a rule that the run only observes: no decompiling, no copying assets, no taps that buy, book, pay or change the user's account.${extra}`
let prop = await agent(COMPARE(''), { label: 'compare:proposals', phase: 'Compare', schema: PROPOSED })
if (!prop) throw new Error('compare failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer. Check ${RD}/RESEARCH.md and ${RD}/PHONE-STUDY.md against the evidence: ${RD}/SOURCES.md, the sources themselves where a claim matters, and the template's code on the branch heads (git show). ${RULES}
The author's summary: ${JSON.stringify(prop)}
Blocking means: a claim about District not backed by its cited source or presented with more certainty than it has; a wrong statement about the template's current code; a proposal that would hurt smoothness or memory on the Redmi without saying so; a proposal that conflicts with REQUIREMENTS or the template's AGENTS.md (no Material, AppTheme only) without saying so; a phone-study step that could buy, book, pay, change the account, or copy the app's code or assets. There is no pressure to find something. Do not modify anything.`, { label: 'review:research', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(COMPARE(`\nThis is a revision. A separate reviewer found these blocking problems in your first version: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in the files and return the full updated result.`), { label: 'compare:revise', phase: 'Review', schema: PROPOSED })
  if (revised) prop = revised
}
return { verified: { stack: verified.stack, report_path: verified.report_path, dropped: verified.dropped.length }, proposals: prop, review }
