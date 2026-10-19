export const meta = {
  name: 'research-telegram',
  description: "Research why Telegram for Android feels so fluid (screen transitions, swipe back, sheets, lists with enormous data) from their open source, and what android-base can take as ideas re-built in Compose: Sonnet source and web inventories, an Opus verifier, an Opus comparison with proposals for the user's approval, a separate Opus review",
  phases: [
    { title: 'Gather', detail: 'three Sonnet readers: transitions and gestures, lists and performance machinery, the web and Compose equivalents' },
    { title: 'Verify', detail: 'an Opus verifier checks every claim against the source' },
    { title: 'Compare', detail: 'Opus gap analysis against the template, proposals, a phone-study procedure' },
    { title: 'Review', detail: 'a separate Opus reviewer, then one revision' },
  ],
}

// The user, Oct 19 23:16: "Over the actual animations look for optimisation."
// The user, Oct 19 23:14: "U can forget exteragram, it's mostly famous for smooth blurs and all, telegram is the goat of animations".
// The user, Oct 19 23:08: "There is this app called exteragram ... known for its good transitioning animations fluidity
// and all. Launch a agent for understanding that app as well and what good bits we can take. It's hella smooth. Even with
// enormous data ... we are always gonna be 100% compose but we can definitely extract the good bits from Java xml ... Even
// the original telegram app is hella smooth and superb animations". REQUIREMENTS 48. The source (GPL-2.0) was downloaded
// with the user's OK into research/telegram/src (Java tree only, read-only). Changes to android-base need the user's
// approval one by one, as with District (REQUIREMENTS 45).
// Model routing (global CLAUDE.md §10): reading source and the web into inventories is mechanical, so Sonnet lists facts
// with file:line; verifying, comparing, proposing and reviewing are Opus.
const OV = 'D:/projects/tools/.overhaul'
const RT = `${OV}/research/telegram`
const TG = `${RT}/src/telegram/TMessagesProj/src/main/java`
const R = 'D:/projects/tools/android-base'

const RULES = `Rules: read-only everywhere except your own files under ${RT}/ (never inside ${RT}/src/). Never run gradle or adb, never build or run the downloaded source, never touch a worktree under ${OV}/wt/ (other runs are editing them), never commit or push. Read android-base code from branch heads with git -C ${R} show <branch>:<path> or git -C ${R} grep. LICENCE: Telegram for Android is GPL-2.0 and android-base is MIT, so take ideas, techniques and measured numbers only: never copy their code into notes, proposals or android-base; describe what the code does in your own words, cite file:line, and record parameter values (durations, curves, thresholds) as facts. Python that reads the downloaded files runs with -I.`
const LOOKUP = `You are a careful reader building an inventory. Report facts only, each with its file:line (or URL) and the parameter values it uses; say plainly when something is a guess. Do not judge what android-base should do. Search the source with grep and read the classes you find. Save your notes to the file named below as you go.`

const FOUND = { type: 'object', properties: {
  findings: { type: 'array', items: { type: 'object', properties: {
    topic: { type: 'string' }, what: { type: 'string' }, where: { type: 'string' }, parameters: { type: 'string' },
    kind: { type: 'string', enum: ['source', 'official', 'third-party', 'guess'] },
  }, required: ['topic', 'what', 'where', 'kind'] } },
  not_found: { type: 'array', items: { type: 'string' } },
  notes_path: { type: 'string' },
}, required: ['findings', 'not_found', 'notes_path'] }
const VERIFIED = { type: 'object', properties: {
  facts: { type: 'array', items: { type: 'object', properties: { claim: { type: 'string' }, where: { type: 'string' }, confidence: { type: 'string', enum: ['confirmed', 'likely', 'unverified'] }, why: { type: 'string' } }, required: ['claim', 'where', 'confidence', 'why'] } },
  dropped: { type: 'array', items: { type: 'string' } },
  report_path: { type: 'string' },
}, required: ['facts', 'dropped', 'report_path'] }
const PROPOSED = { type: 'object', properties: {
  summary: { type: 'string' },
  differences: { type: 'array', items: { type: 'string' } },
  proposals: { type: 'array', items: { type: 'object', properties: {
    id: { type: 'string' }, title: { type: 'string' }, telegram: { type: 'string' }, ours: { type: 'string' },
    change: { type: 'string' }, owner: { type: 'string' }, redmi_cost: { type: 'string' }, effort: { type: 'string' },
    risk: { type: 'string' }, evidence: { type: 'string' }, needs_phone_check: { type: 'boolean' },
  }, required: ['id', 'title', 'telegram', 'ours', 'change', 'owner', 'redmi_cost', 'effort', 'risk', 'evidence', 'needs_phone_check'] } },
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
const gathered = (await parallel([
  () => agent(`${LOOKUP} ${RULES} Notes file: ${RT}/gather-transitions.md. Source: ${TG} (Telegram, current).
Screen transitions and gestures: how a screen is pushed and popped (ActionBarLayout or its successor, INavigationLayout, BaseFragment and its transition callbacks), the swipe-back gesture (where it starts, how far, velocity and distance thresholds, what follows the finger, how the page beneath moves and dims, shadows), Android predictive back if used, the curves and durations (CubicBezierInterpolator constants, SpringAnimation stiffness and damping, ValueAnimator durations), how the chat opens from the chat list and how the profile header expands (DialogsActivity, ChatActivity, ProfileActivity), bottom sheets (BottomSheet and its subclasses: open, drag, fling, dismiss, backdrop), popup menus and context menus, and anything that keeps velocity across a hand-off. For each: what moves, the numbers, file:line, and above all how it is made cheap: what is drawn during the animation (a cached bitmap or snapshot of the page, a hardware layer, a draw-only translation, alpha on a layer), what is not re-measured, re-laid-out or re-bound, which work moves off the main thread, what is pre-built before the gesture starts, and what a slower device tier turns off. FOCUS (the user, Oct 19 23:34: "Over the actual animations look for optimisation"): what matters most is HOW Telegram makes motion and scrolling cheap, more than how the motion looks.`, { label: 'gather:transitions', phase: 'Gather', schema: FOUND, model: 'sonnet' }),
  () => agent(`${LOOKUP} ${RULES} Notes file: ${RT}/gather-lists-perf.md. Source: ${TG} (Telegram, current).
Why it stays smooth with enormous data: RecyclerListView and the item animators (ChatListItemAnimator, the dialogs list animator), how message and chat rows are drawn (custom views drawing text layouts and images directly, StaticLayout caching), image loading off the main thread (ImageReceiver, ImageLoader, decode queues, bitmap caches and recycling), DispatchQueue and background threads, how they avoid allocations and layout passes during scroll and animations (cached Paints, layout reuse, hardware layers during transitions, invalidation scope), device performance tiers (SharedConfig device performance class, LiteMode: what each tier turns off), high refresh rate handling, and helpers such as AnimatedFloat or AnimatedTextView. For each: what it does, the numbers, file:line, and what it saves per frame (allocations, layout passes, overdraw, main-thread work). FOCUS (the user, Oct 19 23:34: "Over the actual animations look for optimisation"): what matters most is HOW Telegram makes motion and scrolling cheap, more than how the motion looks. Also note how they measure and guard performance (frame-time checks, StrictMode, internal flags) if the code shows it.`, { label: 'gather:lists-perf', phase: 'Gather', schema: FOUND, model: 'sonnet' }),
  () => agent(`${LOOKUP} ${RULES} Notes file: ${RT}/gather-web.md. Source for cross-checks: ${TG} (Telegram).
The web: what Telegram's Android developers or well-informed third parties have published about how Telegram Android achieves its smoothness (architecture, custom views instead of the framework's, animation choices, release notes that describe new animations), and how each main technique maps to Jetpack Compose today (official Android docs first: SharedTransitionLayout, graphicsLayer and CompositingStrategy, Animatable and springs, LazyColumn animateItem, predictive back with SeekableTransitionState, Modifier.drawWithCache), with dates and URLs.`, { label: 'gather:web', phase: 'Gather', schema: FOUND, model: 'sonnet' }),
])).filter(Boolean)
if (!gathered.length) throw new Error('every reader failed')
if (gathered.length < 3) log(`${3 - gathered.length} reader(s) failed; verifying what came back`)

phase('Verify')
const verified = await agent(`You are the Opus verifier of three Sonnet inventories of how Telegram for Android achieves its smoothness. Trust nothing until you check it. ${RULES}
Their findings: ${JSON.stringify(gathered)}
Open the source at every file:line that matters (each transition, gesture threshold, curve, duration, spring, item animator, performance tier and threading claim) and mark each confirmed, likely or unverified with why; correct wrong numbers; drop what the code does not do. Write ${RT}/SOURCES.md: the verified facts grouped by topic, each with file:line or URL, in your own words (no copied code).`, { label: 'verify:sources', phase: 'Verify', schema: VERIFIED })
if (!verified) throw new Error('verifier failed')

phase('Compare')
const COMPARE = (extra) => `You are the motion and performance lead of android-base. ${RULES}
The user loves how fluid Telegram for Android feels (screen transitions, swipe back, and smoothness with enormous data) and wants to know what they do differently and what android-base, which stays 100 % Jetpack Compose, can take from them; every change you suggest needs the user's approval one by one, so you propose and implement nothing. The template's goal (the user, Oct 19): light and smooth, the most optimised visual feel, judged on a budget Redmi Note 10 Pro Max at 120 Hz. Verified facts: ${JSON.stringify(verified)}.
Read the template's own motion and performance work first: ${OV}/research/MOTION-PLAN.md, ${OV}/research/SHEET-SPEC.md, ${OV}/research/SMOOTHNESS-PLAN.md, ${OV}/research/back-google/DESIGN.md, ${OV}/research/district/RESEARCH.md (to avoid repeating its proposals), ${OV}/REQUIREMENTS.md (19, 21, 22, 24, 28, 29, 42), and the code on the branch heads wp/WP03 (design system, AppMotion springs, AppSheet, lists), wp/WP04 (navigation shell, back card, push and pop), wp/WP05-glass, and the performance overlay on wp/HUD.
Then write ${RT}/RESEARCH.md, plain English for the user first and engineering detail after. FOCUS (the user, Oct 19 23:34: "Over the actual animations look for optimisation"): what matters most is HOW Telegram makes motion and scrolling cheap, more than how the motion looks. So lead with the optimisation techniques and their Compose equivalents (what each saves per frame on the Redmi, and whether android-base already does it, with file:line), and put pure look-and-feel parameters after. Cover: what Telegram does differently (and why it feels smooth, including with enormous data), what android-base already does as well or better, and the proposals. Each proposal: what Telegram does (file:line), what we do today (file:line), the change expressed as Compose, the owning package, the cost on the Redmi (frames, memory, overdraw), effort, risk, the evidence, and whether it needs a phone check. Order them by how much smoothness they would add on the Redmi (frame time, jank, hitches, memory), then by how much the user would feel them. Leave out anything that would cost smoothness on the Redmi, and say why. Never propose copying their code: the licence is GPL-2.0 and android-base is MIT.
Also write ${RT}/PHONE-STUDY.md for a later run once the user has Telegram installed and signed in: the same measuring approach as ${OV}/research/district/PHONE-STUDY.md (recordings, gfxinfo frame stats, curve fits against the template's catalog), with privacy rules that fit a messenger: record only screens without anyone's messages or names (settings, the user's own empty test chat or a public channel they choose), keep recordings local and delete them after measuring, never send, react, join, call or change anything.${extra}`
let prop = await agent(COMPARE(''), { label: 'compare:proposals', phase: 'Compare', schema: PROPOSED })
if (!prop) throw new Error('compare failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer. Check ${RT}/RESEARCH.md and ${RT}/PHONE-STUDY.md against the evidence: ${RT}/SOURCES.md, the source itself at the cited lines where a claim matters, and the template's code on the branch heads (git show). ${RULES}
The author's summary: ${JSON.stringify(prop)}
Blocking means: a claim about Telegram the source does not support; a wrong statement about the template's code; copied GPL code or a proposal to copy it; a proposal that would hurt smoothness or memory on the Redmi without saying so, or that conflicts with REQUIREMENTS or the template's AGENTS.md (no Material, AppTheme only, 100 % Compose); a duplicate of a District proposal not marked as such; a phone-study step that could expose anyone's messages or names, or send, join or change anything. There is no pressure to find something. Do not modify anything.`, { label: 'review:research', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(COMPARE(`\nThis is a revision. A separate reviewer found these blocking problems in your first version: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in the files and return the full updated result.`), { label: 'compare:revise', phase: 'Review', schema: PROPOSED })
  if (revised) prop = revised
}
return { verified: { report_path: verified.report_path, dropped: verified.dropped.length }, proposals: prop, review }
