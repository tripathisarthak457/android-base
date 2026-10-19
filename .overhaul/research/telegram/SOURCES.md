# Telegram for Android: how it stays smooth (verified sources)

Verifier pass over `gather-transitions.md`, `gather-lists-perf.md` and `gather-web.md` (2026-10-20).
Every source claim below was re-opened at the cited file:line in the local clone unless it is marked otherwise.

**Status tags**
- **[C] confirmed**: I read the cited lines and they say this.
- **[L] likely**: partly read, or taken from a page the gatherer fetched and I did not re-fetch, or a framework rule I am applying from memory.
- **[U] unverified**: not checked, or it cannot be checked from this download.
- **[CALC]**: arithmetic on confirmed numbers. **[FW]**: Android framework behaviour that is not in this source.

**Licence.** Telegram for Android is GPL-2.0 and android-base is MIT. This file describes behaviour in my own words and records numbers only. No Telegram code is copied here, and none may be copied into android-base.

**Paths.** `T` = `D:/projects/tools/.overhaul/research/telegram/src/telegram/TMessagesProj/src/main/java/org/telegram`.
`ABL` = `T/ui/ActionBar/ActionBarLayout.java`, `BS` = `T/ui/ActionBar/BottomSheet.java`, `CA` = `T/ui/ChatActivity.java`,
`NC` = `T/messenger/NotificationCenter.java`, `C/` = `T/ui/Components/`, `M/` = `T/messenger/`.

---

## 0. Corrections to the three inventories

These are the places where the inventories were wrong or incomplete. The rest of this file already uses the corrected versions.

1. **Predictive-back scale pivot.** The pivot sits dp(82) from the edge *opposite* the touch, not from the touch side. A swipe from the left edge shrinks the page toward its right side (ABL:1133). During the gesture, drawChild also translates the canvas back by the page's translationX, up to dp(56) (ABL:1123-1130). So while the finger is down, the page **scales but does not slide**. It slides out only after release, past dp(56).
2. **Predictive-back scale range.** The scale goes from 1.0 toward 0.90 as translation reaches dp(56), and keeps going toward 0.85 as the page alpha drops (ABL:1119). The inventories said "0.90".
3. **Classic swipe release curve.** The settle animators have no interpolator, so the framework default applies, which is AccelerateDecelerate [FW] (ABL:1606-1630). The inventories said "AnimatorSet default".
4. **BottomSheet right-hand variant.** A second setInterpolator call after the branch overwrites the open curve, so the open curve is the sheet's `openInterpolator` (EASE_OUT_QUINT by default), not DEFAULT. The duration is 250 ms (BS:1771-1779). The dismiss of that variant really is 200 ms DEFAULT (BS:2061-2063).
5. **BottomSheet dismiss.** A normal dismiss is **250 ms EASE_OUT** (BS:2065-2066). 180 ms (330 ms for the call sheet) applies only to `dismissWithButtonClick` (BS:1903-1904). The lists-perf inventory said "dismiss 180 ms".
6. **Keyboard: the IME-insets path is dead in release.** It needs `USE_ANDROID11_INSET_ANIMATOR`, which is false (AdjustPanLayoutHelper.java:39). All four constructions use the default constructor (CA:17210, ChatAttachAlert.java:1398, ShareAlert.java:546, ChatCustomReactionsEditActivity.java:172). The lists-perf inventory said it was used on 90 Hz+ API 30+ devices. Only the 250 ms animator runs.
7. **Chat blur RenderNode re-recording.** By default the node is re-recorded on every draw, at most once per frame (SizeNotifierFrameLayout.java:1022-1025, 994, 835-836). It is re-recorded only on child invalidation only where `invalidateOptimized()` returns true, which is ChatActivity's and DialogsActivity's content views (CA:17376, DialogsActivity.java:1130). Those register invalidate listeners on the recorded children (SizeNotifierFrameLayout.java:916-930).
8. **Lottie precache rule.** Precache is on when the filter says `pcache`, or when the filter lacks `nolimit` and the device is not HIGH (ImageLoader.java:936-940). The lists-perf wording "unless tier HIGH and nolimit" was wrong.
9. **Wallpaper motion clock.** A dt over 20 ms is replaced by 17 ms. Values up to 20 ms pass through (MotionBackgroundDrawable.java:654-655). It is not a "cap at 17".
10. **BottomSheet.makeAttached** has 11 live call sites plus 1 commented out, not 13.
11. **bugs.telegram.org/c/15403.** The reply calling tier gating "intended behaviour" (2022-06-05) is from a poster the page does not identify as staff. The device-class description there is a user's research.
12. **The swipe-dismiss "fast dismiss" flag is dead.** `useFastDismiss` is set at BS:388 and never read. A swiped-away sheet uses the normal 250 ms dismiss, and velocity does not change its duration.
13. **The side swipe-back of a sheet reads velocity without computing it.** It reads velocity without calling computeCurrentVelocity first (BS:467-473), unlike the vertical path (BS:568). [FW] The values are then those of the last compute, so the 3500 px/s fling shortcut is unlikely ever to fire there. [L]

---

## 1. Provenance

- **[C]** The clone is DrKLO/Telegram 13.0.2, code 7175, HEAD f32ffeec dated 2026-10-13 ("update to 13.0.2 (7175)"). It is a shallow clone (gradle.properties:16-17; git log; is-shallow = true).
- **[C]** Only `AndroidManifest.xml` and `java/` exist under `TMessagesProj/src/main`. `res/` and `jni/` are absent, so XML drawables (layer_shadow, header_shadow), anim resources and native code (rlottie, stackBlur, ffmpeg) are unknown.
- **[C]** `src/exteragram` is not on disk. Every exteraGram claim in gather-transitions §0 (LNavigation spring navigation, 9.6.6 snapshot) is carried over **unverified**.
- **[C]** There is no Compose anywhere in the Java tree (grep for androidx.compose returns 0 files). androidx.dynamicanimation 1.0.0 is a dependency (TMessagesProj/build.gradle:48). R8 full mode is on (gradle.properties:30). There is no profileinstaller, baseline-profile, JankStats or tracing dependency.

## 2. Page stack: ActionBarLayout

### 2.1 Structure
- **[C]** There are two page containers, swapped by reference on push, pop and committed swipe. Nothing is re-inflated (ABL:1306-1309 on swipe end; ABL:2076-2078 and 2571-2573 per inventory, not re-read). A fragment keeps its view tree while it is detached. On swipe start the previous page's view is re-attached, and is created only if it is null (ABL:1365-1379).
- **[C]** After a push, the page below is detached with removeViewInLayout, which avoids an extra layout request (ABL:1768-1805, removeViewInLayout at 1785, 1799, 1336, 1342). At rest only one page is in the tree.
- **[C]** hasOverlappingRendering returns false on ABL (ABL:3308-3311). The inner page container returns true on SDK 28+ (ABL:250-256).
  - [FW] So on SDK 28+, a page whose container alpha is below 1 is drawn through an offscreen buffer for the whole fade.
- **[C]** ABL intercepts all touches while an animation runs (ABL:1039-1042). A watchdog force-finishes any transition older than 1500 ms (ABL:1747-1749).

### 2.2 Push and pop by button or API
- **[C]** The animation is a self-reposting UI-thread runnable, not an Animator (ABL:1807-1917). AndroidUtilities.runOnUIThread with no delay is a plain Handler.post (AndroidUtilities.java:2935-2943).
- **[C]** Timing comes from the wall clock in ms. On the first tick, a dt over 40 ms becomes 0. After that, dt is clamped to 18 ms. Progress grows by dt/duration (ABL:1822-1831).
  - [CALC] A hitch costs at most 18 ms of progress, so the motion slows rather than jumping.
- **[L]/[FW]** Each tick changes view properties, and each change invalidates. ViewRootImpl then posts a sync barrier that holds back ordinary Handler messages until the next traversal. So the loop effectively runs once per frame, right after each draw. This pacing is not stated in the source.
- **[C]** Duration is 150 ms, or 190 ms for a preview open. The curve is DecelerateInterpolator(1.5) (ABL:614, 1830, 1873).
- **[C]** Open: the new page goes from alpha 0 to 1 and from translationX dp(48) to 0 (ABL:1876-1892). It starts at a raw 48 px (ABL:2222), which is invisible because alpha is 0.
- **[C]** Close: the leaving page goes from alpha 1 to 0 and from translationX 0 to dp(48) (ABL:1895-1907). The page underneath never moves. No layer type is set on this path.
- **[C]** Every tick calls onTransitionAnimationProgress on both fragments. The navigation-bar colour blends at 4x speed, so it is done at 25 % progress (ABL:1835-1864).
- **[C]** The animation is skipped when the `view_animations` pref is false, which defaults to true (ABL:1997).
- **[C]** Tablet "layers" mode animates the whole layout's alpha from 0 to 1 and scale from 0.9 to 1 over 200 ms with EASE_OUT_QUINT (ABL:2156-2170). Close is 200 ms AccelerateDecelerate (ABL:2717-2718).
- **[C]** Preview open uses Overshoot(1.02) with scale 0.7 to 1. Preview close uses EASE_OUT_QUINT with scale 1 to 0.9 (ABL:615, 1866-1900).

### 2.3 Waiting for the new page to be ready
- **[C]** If the incoming fragment reports needDelayOpenAnimation, the slide waits until the fragment calls resumeDelayedFragmentAnimation or 200 ms pass (ABL:1920-1928, 2270-2282).
- **[C]** If the keyboard is visible in either container, the open waits 250 ms (ABL:2226-2269), or less if a layout shows the keyboard closed (ABL:396-400). Close waits 200 ms (ABL:2665-2676). The keyboard wait and the needDelay wait chain: 250 ms first, then up to 200 ms more (ABL:2243-2249).
- **[C]** ChatActivity delays while `firstLoading`, except when the page below is a chat with the keyboard up (CA:27141-27149). ProfileActivity delays when playProfileAnimation is 0 (ProfileActivity.java:9534-9539).
- **[C]** ChatActivity releases the wait as soon as its first messages arrive (CA:20736, 21846-21853).
- **[C]** 7 classes override needDelayOpenAnimation; BaseFragment holds the default. The bodies of the 5 not named above were not read.

### 2.4 Classic swipe-back
- **[C]** The swipe can start anywhere on the page. It needs: a stack deeper than 1, no running animation, no action mode, no predictive back in progress, and the top fragment's isSwipeBackEnabled (ABL:1413-1431).
- **[C]** Recognition: rightward dx of at least 0.4 cm, measured with the screen's xdpi (AndroidUtilities.java:2902-2904), and dx/3 greater than |dy|. canBeginSlide must also hold, and no horizontally scrollable child may be under the finger (ABL:1439-1446). The start x is reset at recognition, so the page does not jump (ABL:1442).
- **[C]** The page follows the finger 1:1 through translationX. setInnerTranslationX only stores the value, invalidates, and reports slide progress to both fragments (ABL:1460-1461, 956-970).
- **[C]** A fling of at least 3500 px/s, mostly horizontal, starts the swipe even if the slop was never crossed (ABL:1471-1483).
- **[C]** On release the swipe cancels when x is below width/3, unless velX is at least 3500 and |velX| is at least |velY| (ABL:1489).
- **[C]** Settle: a commit takes max(200 ms x remaining/width, 50 ms). A cancel takes max(320 ms x x/width, 120 ms) (ABL:1606-1630). There is no interpolator, so AccelerateDecelerate [FW] applies. Velocity is used only for the commit-or-cancel decision, never as an animation input.
- **[C]** On the first accepted move, prepareForMoving re-attaches the previous page (creating it if needed), calls its onResume, and puts a **hardware layer on the top page only** (ABL:1358-1409, layer at 1404). The first tracked move hides the keyboard and calls onBeginSlide, which dismisses a visible dialog (ABL:1447-1455; BaseFragment.java:727-739).
- **[C]** At the end, the layer is removed and containers are swapped or the previous view detached (ABL:1291-1356). That setLayerType at 1404 is the only HARDWARE call in ABL. All the others set NONE (1354, 1421, 1496, 1508).

### 2.5 What is drawn during a slide (ABL.drawChild 1084-1217)
- **[C]** Each page is clipped to its visible strip with clipRect, while the finger tracks only. The clip is skipped during button transitions and post-release settles (ABL:1100-1103, 1170-1173, 1764-1766).
- **[C]** The page beneath gets one black scrim rect. Its alpha is 120 x clamp(visibleWidth/width, 0, 0.8), at most 96 of 255 (ABL:1204-1211).
- **[C]** The edge shadow drawable is drawn only below SDK 31 or for sheets. Its alpha ramps over dp(20) (ABL:1180-1202).
- **[C]** On SDK 31+ the front page is instead clipped with a rounded path that uses the display's own corner radii from WindowInsets (ABL:1111-1155).

### 2.6 Predictive back (API 34+)
- **[C]** The manifest sets enableOnBackInvokedCallback (AndroidManifest.xml:112).
- **[C]** LaunchActivity registers an OnBackAnimationCallback on SDK 34+ and a plain callback on SDK 33 (LaunchActivity.java:738-851).
- **[C]** The layout's back start is deferred until system progress passes 0.015. Progress is then remapped to (p-0.015)/(1-0.015) (LaunchActivity.java:787-806).
- **[C]** A notification lock is held from start to invoke or cancel (LaunchActivity.java:741, 780-783, 750-753, 813-816).
- **[C]** Tablets skip all of this (LaunchActivity.java:755, 776, 803, 818).
- **[C]** Translation is dp(56) x StandardDecelerate(progress) (ABL:1570). This is cancelled visually during the gesture, as described in §0.1.
- **[C]** Scale goes from 1 toward 0.90, and toward 0.85 as alpha drops. The pivot Y is the touch Y (ABL:1119, 1133). The page beneath is left unclipped (ABL:1163-1165).
- **[C]** Commit animates to width + dp(56) over max(200 ms x dist/width, 380 ms). Cancel takes max(320 ms x x/width, 320 ms). Both use EASE_OUT_QUINT (ABL:1606-1628).
- **[C]** Slide progress reported to the previous fragment is clamp01(value / (6 x dp(56))) (ABL:963-964).

### 2.7 Custom per-fragment transitions
- **[C]** A fragment may return its own AnimatorSet from onCustomTransitionAnimation (BaseFragment; used at ABL:2662).
- **[C]** ChatActivity does this for two cases:
  - pull-down-to-next-chat: 300 ms DEFAULT (CA:43249, 43345-43346).
  - topics to chat: 150 ms (CA:43352, 43404).
  - Locks are released 32 ms after the end (CA:43320, 43400). Otherwise it returns null (CA:43413).

## 3. Deferring main-thread work while something moves

### 3.1 NotificationCenter animation gate
- **[C]** setAnimationInProgress registers a token with an allow-list (NC:463-487).
- **[C]** While any token is live, a post is delivered only if every live token's allow-list contains its id. A token with a null list blocks everything. Blocked posts are queued and replayed in order when the count reaches 0 (NC:586-614, 662-666, 526-568).
- **[C]** Always allowed: start/stopAllHeavyOperations, didReplacedPhotoInMemCache, closeChats, invalidateMotionBackground, needCheckSystemBarColors, messageReceivedByServer2 (NC:587).
- **[C]** doOnIdle runnables also wait for the gate (NC:919-925).
- **[C]** Expiry: a sweep scheduled 5017 ms after the first token releases tokens older than 1000 ms. Posting also expires tokens older than 5017 ms (NC:35, 482-484, 489-517, 595-600).
- **[C]** AnimationNotificationsLocker takes a token on both the account center and the global center (AnimationNotificationsLocker.java:22-41). 40 files reference it, in 84 lines.
- **[C]** ChatActivity:
  - takes a **global** token with an empty allow-list in onFragmentCreate (CA:2908), so the global center is fully gated from creation until the open animation ends (CA:27405).
  - takes a per-account token per transition. On the first open it allows messagesDidLoad, so the first page can arrive mid-slide (CA:27355-27383).
  - while loading before showing, a token with a wider list is force-released after 800 ms (CA:20638-20646).
  - postpones didReceiveNewMessages for its own dialog while firstLoading (CA:1298-1309, registered at 2916).
- **[C]** The chat item animator holds an account token with a 19-id allow-list, without the heavy-ops stop (CA:1266-1287, 6492-6500). Its onAnimationStart also starts any delayed keyboard animation, so both move together (CA:6508).
- **[C]** ProfileActivity holds a token during open (ProfileActivity.java:9730-9735).

### 3.2 Heavy-operations pause (flag 512)
- **[C]** The first token posts stopAllHeavyOperations(512). The last release posts start (NC:467-478, 530-535).
- **[C]** ImageReceiver stops Lottie drawables marked heavy (the base class always reports heavy, RLottieDrawable.java:947-949) and GIF/video drawables. Layers at or below the receiver's own layer number are ignored. Late attachers read the current mask, and a receiver attaching during a gate pauses itself (ImageReceiver.java:1195-1233, 3159-3195).
- **[C]** DrawingInBackgroundThreadDrawable pauses too, but ignores 512 on HIGH tier (DrawingInBackgroundThreadDrawable.java:232-257).
- **[C]** Pause-on-scroll in RecyclerListView is opt-in (RecyclerListView.java:1626-1638, 3099-3101). Only the DialogsActivity list pages opt in (DialogsActivity.java:4068).
- **[C]** The chat list posts stop/start on scroll only on LOW tier (CA:6838-6841, 6866-6869). It posts stopSpoilers/startSpoilers on every tier (CA:6842, 6870).
- **[C]** BottomSheet posts the pause on open, on dismiss and on snap-back (BS:1816-1818, 2093, 416).

## 4. Device tiers and Lite Mode

### 4.1 Performance class
- **[C]** The class is measured once per process, and can be overridden from settings (SharedConfig.java:1625-1633).
- **[C]** An SoC hash deny list forces LOW on SDK 31+ (SharedConfig.java:1640-1647, list at 360+: Exynos 850, 7872, ...).
- **[C]** Frequency input: the **mean** of cpuinfo_max_freq over cores, in MHz (1649-1662).
- **[C]** LOW if any of: SDK < 21; cores <= 2; memoryClass <= 100; cores <= 4 with freq <= 1250; two old-SDK combos; total RAM < 2 GiB (1672-1681).
- **[C]** AVERAGE if any of: cores < 8; memoryClass <= 160; freq <= 2055; unknown freq with 8 cores on SDK <= 23 (1682-1688). Otherwise HIGH.
- **[CALC/U]** The user's Redmi (SD732G, spec clocks 2 x 2.3 + 6 x 1.8 GHz) gives a mean of about 1.93 GHz, so it is **AVERAGE**. This comes from spec-sheet clocks. Sysfs, memoryClass and SOC_MODEL on the custom ROM were not read.

### 4.2 Lite Mode flags
- **[C]** Flags: stickers 1/2, emoji 4/8/16/4096/8192/16384, background 32, forum 64, spoiler 128, blur 256, calls 512, video 1024, gif 2048, scale 32768, thanos 65536, particles 1<<17, liquid glass 1<<18 (LiteMode.java:23-50).
- **[C]** Presets:
  - LOW = 198684 (premium-only emoji variants, GIF autoplay, thanos, particles).
  - MEDIUM = 204383 (adds both sticker flags, all chat emoji, forum two-column, call animations, video autoplay).
  - HIGH = 262143 (bits 0-17, no liquid glass).
  - Background, spoiler, blur and scale are missing from LOW and MEDIUM (52-85).
- **[C]** The default preset follows the tier (202-210).
- **[C]** The server can replace the presets and the battery thresholds through the JSON keys settings_mask and battery_low (176-200).
- **[C]** Power saver returns mask 0 at or below a battery level, default 10. The battery level is cached for 12 s (88-90, 102-134).
- **[C]** canBlurChat needs AVERAGE on SDK 31+, or HIGH below 31. chatBlurEnabled also needs FLAG_CHAT_BLUR (SharedConfig.java:1734-1740).
  - [CALC] On an AVERAGE phone with default settings, chat blur is off because MEDIUM lacks the flag.

### 4.3 Tier-scaled workloads
- **[C]** RenderNode blur radius 60/4/3 and record downscale density/dp(12)/dp(15) for HIGH/AVERAGE/LOW (SizeNotifierFrameLayout.java:937-959).
  - [CALC] In content pixels at density 2.75 this is about 165/132/124 px of blur, so the look is similar and the cost much lower.
- **[C]** Thanos particles 120k/60k/30k (ThanosEffect.java:1112-1123). Spoiler particles 30 per character on HIGH and 10 otherwise, capped at 150 or 100 (SpoilerEffect.java:113-133).
- **[C]** The first chat page is 25 messages on HIGH and 15 otherwise (30/20 for threads) (CA:3131-3138). Animated emoji per text: 100 on HIGH, 50 otherwise (Emoji.java:608, 687).
- **[C]** Decoded images use RGB_565 unless force-8888 applies. canForce8888 means memoryClass >= 192 (ImageLoader.java:2077, 1302).

### 4.4 Parallax of the page beneath (tier-gated, only in three fragments)
- **[C]** Only DialogsActivity, TopicsFragment and MainTabsActivity (which forwards) implement onSlideProgress (grep). ABL itself never moves the page beneath.
- **[C]** DialogsActivity: nothing on LOW.
  - On AVERAGE, or when the scale flag is off: translation only, -dp(40) x (1-p).
  - On HIGH with the scale flag: scale 1 - 0.05(1-p) plus a -dp(4)(1-p) shift, pivot at the left edge. Hardware layers go on viewPages[0] and the ActionBar, and child clipping is off for the slide.
  - The pages are drawn with canvas translate/scale in drawChild, which needs only an invalidate (DialogsActivity.java:12528-12671, 888-930).
  - The commit animator is clamp(200 ms x dist/width, 80..200 ms) with EASE_OUT (12540-12547).
- **[C]** TopicsFragment, on any tier except LOW: hardware layer on the content view, plus scale 0.95 on the list and ActionBar. It requests a layout at slide start and end (TopicsFragment.java:3933-3985).

## 5. Opening specific screens

### 5.1 Chat
- **[C]** The first message load starts in onFragmentCreate (CA:2716 onward, firstLoadMessages at 3138, defined 3298-3330), which runs before createView (CA:3616). ABL calls onFragmentCreate before performCreateView.
- **[C]** The DB read is on storageQueue and the processing on stageQueue (MessagesStorage.java:9997-10009). The MessageObject, layouts included, is built there before the UI post (MessagesController.java:12082-12087, 12168).
  - So for the cache path, the disk read overlaps view inflation. The network path was not traced.
- **[C]** The item animator is installed only for animated updates after fragmentOpened. Otherwise it is set to null (CA:38409-38419). It is refused while a fast-scroll animation runs (CA:4840-4845). The loading spinner animates only once the fragment is open (CA:11943).

### 5.2 Profile
- **[C]** Chat to profile uses a custom AnimatorSet: 250 ms CubicBezier DEFAULT for an avatar tap (mode 2), 180 ms Decelerate(1.0) for the title tap (mode 1) (ProfileActivity.java:9956, 10266).
- **[C]** It puts a hardware layer on listView (9957, removed 10243). The previous chat's ActionBar skips drawing its children (9944). The set starts 50 ms after creation (10268).
- **[C]** Mode 2 calls needLayout every tick (9872). That resizes the avatar container through LayoutParams and requests a layout per frame (8627-8693).
- **[C]** Before navigating, the chat header puts the avatar bitmap into the memory cache, hands over a shared-media preloader and passes the user/chat info objects (ChatAvatarContainer.java:249-252, 575-582, 626-650).
- **[C]** Pull-down expand starts at 33 % expandProgress. Duration = (1-v) x 250 ms / factor, where factor = clamp(|vy|, dp(1100), dp(2000)) / dp(1100), giving 250 to about 137 ms [CALC]. The curve is EASE_BOTH, or FastOutSlowIn x 1.3 when stories exist (ProfileActivity.java:8444-8482). Collapse uses EASE_BOTH (8570-8578). Header snap uses smoothScrollBy with EASE_OUT_QUINT (4204-4211).

### 5.3 Media viewer
- **[C]** A proxy image view holds the thumbnail. Two keyframe arrays of 13 values are computed once in a pre-draw listener, and a single progress property drives them (PhotoViewer.java:1915, 17651-17719, 17799).
- **[C]** Open lasts 200 ms with the default curve. The animator starts one post later, with the container on a hardware layer for the run (17796-17838, 17757).
- **[C]** Drag-down close starts at dy >= dp(30) with dy/2 > dx (19115). It closes past containerHeight/6 (19247). Page change happens past 1/3 width or above dp(650)/s (19274-19278). animateTo is 250 ms Decelerate(1.5) (19359-19360, 2107).

## 6. Sheets, popups, scrims, keyboard

### 6.1 BottomSheet
- **[C]** It is a Dialog with window animations disabled (BS:86, 1357), or a view attached inside the fragment container (BS:1340-1353).
- **[C]** show() measures first and parks the sheet offscreen. The open starts when onLayout has run twice, or after a fallback of 150 ms (0 with openNoDelay, 500 when waiting for the keyboard) (BS:1591-1618, 682, 782-786).
- **[C]** Open: 400 ms EASE_OUT_QUINT, start delay 20 ms (0 when keyboard-waiting). It animates translation, dim (alpha 51/255) and nav-bar alpha, with a hardware layer on the container that is removed at the end. It takes a notification lock and pauses heavy ops (BS:213-214, 222, 1728-1819).
- **[C]** Vertical drag starts when dy > 0, dy/3 > |dx| and |dy| >= touchSlop, then follows the finger 1:1 (BS:542-563).
- **[C]** Release snaps back if translation < 0.8 cm and the downward fling is under 3500 px/s or not dominant, or on an upward fling of 3500 px/s or more. Snap-back takes 250 ms x ty/0.8 cm with DEFAULT. Otherwise the sheet dismisses at 250 ms EASE_OUT (BS:382-418, 2065-2066).
- **[C]** Side swipe-back: dx/3 > |dy| and touchSlop. Cancel takes 220 ms DEFAULT, commit 320 ms EASE_OUT_QUINT with a dim fade (BS:442-521).

### 6.2 Attach sheet
- **[C]** The attach sheet replaces the open with a SpringAnimation on translationY (damping 0.75, stiffness 350) plus 400 ms tweens for the dim and button progress. It holds a hardware layer and a lock, and finishes when both are done (ChatAttachAlert.java:5345-5440).
- **[C]** The snap-back after a cancelled discard uses damping 1.5, stiffness 1500 (7019-7022).
- **[C]** The bot web-view sheet drives its action-bar attach progress (not the sheet position) with a no-bounce spring of stiffness 1200, retargeted when the drag crosses half-way (BotWebViewAttachedSheet.java:732-759).

### 6.3 Popup menus
- **[C]** Each popup is a real PopupWindow (ActionBarPopupWindow.java:62).
- **[C]** Open animates only the drawn background scale (0 to final) and its alpha, plus a per-item cascade: alpha and translateY -dp(6) to 0, with a wave of 4 items. Duration is 150 + 16 x visibleItems ms with the default curve, pivot at top-right (843-913; cascade in AndroidUtilities.java:5225-5230).
- **[C]** Alternative per-item start: 180 ms Decelerate (380-397).
- **[C]** Dismiss: 150 ms, either translate ±dp(5) with alpha or scale 0.8 with alpha (69, 1024-1091).
- **[C]** Sub-menu swipe-back: GestureDetector with slop and a 1.5:1 ratio. Duration = 300 ms x max(0.5, |Δ| - min(0.2, v/6000)). A fling starts at 600 px/s and the commit point is 0.5. The curve is DEFAULT and a lock is held (PopupSwipeBackLayout.java:35, 79-107, 263-324).

### 6.4 Long-press scrim and blur snapshot
- **[C]** The chat scrim is a float animated over 320 ms EASE_OUT_QUINT. Its listener only invalidates (CA:10838-10879).
- **[C]** The global blur snapshot draws every window into a bitmap at 1/15 scale and stack-blurs it with radius max(15, max(w,h)/180). It runs **synchronously** on the calling thread, and the background-queue version is commented out (AndroidUtilities.java:6032-6100; ScrimOptions.java:349-368).
- **[C]** ItemOptions animates its dim view over 380 ms EASE_OUT_QUINT (150 ms when the scrim cannot move). A pre-draw listener invalidates the dim view every frame (ItemOptions.java:1310-1347, 1800-1803).

### 6.5 Keyboard (AdjustPanLayoutHelper)
- **[C]** A pre-draw listener catches the one-step window resize and cancels that frame. Height changes under dp(20) are ignored (AdjustPanLayoutHelper.java:89-125).
- **[C]** It sets the heights of views from the content down to the resizable view to max(old, new), requests one layout, then animates only the parent's translationY. At the end it restores MATCH_PARENT and lays out once more (164-254).
- **[C]** Timing is 250 ms on the chat item-animator curve (0.1992, 0.0106, 0.2792, 0.9103) (42-43, 149-150).
- **[C]** It holds a lock (152, 225). Start can be delayed 100 ms so it starts with the chat item animation (153-156, 399-406; CA:6508).

## 7. Gestures in lists and pagers

- **[C]** Tab pager (ViewPagerFixed):
  - Slop 0.3 cm (181). A drag starts when |dx| >= slop and |dx| > dy (656-659). A fling start needs 3000 px/s (691).
  - Cancel when |x| < w/3 and the fling is slow (under 3500) or not dominant (717).
  - Settle time = 4 x round(1000 x distance/|v|), where the distance uses a sine "distance influence" term, or (pageDelta+1) x 100 ms at zero velocity. It is clamped to 150..600 ms with a quintic ease-out (86-89, 760-774, 874-878).
  - [L] The duration formula and quintic curve match stock androidx ViewPager [FW, from memory], with an added 150 ms floor.
  - Tab tap: 540 ms EASE_OUT_QUINT (285-292). A lock is held (544, 800).
- **[C]** Swipe to reply:
  - Starts only when the list is idle, with dx <= -0.4 cm and |dx|/3 > dy. The offset is clamped to -dp(80)..0 (CA:5064-5066).
  - Haptic and reply trigger at |offset| >= dp(50) (5079-5082, 5100). Vertical list scrolling is locked while tracking (5071).
  - Cell offset uses the View translationX (ChatMessageCell.java:26940-26966).
  - Release settle is advanced in onDraw over 180 ms with Decelerate(1.0) (CA:5262-5292).
  - Three springs drive the icon: stiffness 1500 no-bounce with range 0..2000, 400 medium-bounce, and 200 no-bounce. Their listeners only invalidate (CA:4690-4716).
- **[C]** Velocity hand-off into springs appears only on secondary elements:
  - Bulletin swipe-away: above 2000 px/s, a no-bounce spring of stiffness 100 with the fling velocity, aimed at 2 x width, cancelled once off-screen (Bulletin.java:581-616). In its alpha branch the code reconfigures the wrong spring, to stiffness 10, so the alpha spring keeps its defaults (608-610).
  - PiP: low-bounce springs (0.75 damping) at stiffness 650. The X target is chosen by side from x + w/2 + vx/7, and the Y target is y + vy/10. **The Y spring is given vx as its start velocity**, which looks like a bug (PipVideoOverlay.java:597-607, 816-829).
  - No spring is used in page push/pop, page swipe-back, standard sheet open/close or popups.
- **[C]** 40 files reference SpringAnimation.

## 8. Draw-driven animation and frame scheduling

- **[C]** AnimatedFloat has no Animator. The owner calls set(target) from draw. A change above 0.0001 restarts from the current value at SystemClock.elapsedRealtime. The value is a time-based lerp through the interpolator, default 200 ms CubicBezier DEFAULT. It invalidates the parent only while running. The first set snaps (AnimatedFloat.java:18-22, 149-187).
  - **No velocity is carried on retarget**, so a mid-flight retarget changes speed abruptly [CALC from code].
  - There are 407 constructions in 168 files (171 files reference the class).
- **[C]** CubicBezierInterpolator solves x(t) by Newton iteration, up to 13 steps with a 1e-3 tolerance (CubicBezierInterpolator.java:61-72). Its shared static instances hold mutable scratch points, so they are for the UI thread only.
- **[C]** Choreographer60FpsContent is one Choreographer callback that accumulates vsync time and dispatches at most 60 ticks/s. Callbacks subscribe at 1..60 fps on fixed schedules derived only from the fps value, so same-fps callbacks fire on the same tick. View/drawable invalidations are batched per tick, and there is a 30 fps list (Choreographer60FpsContent.java:28-40, 231-300, 324-371).
  - [C] **It re-posts itself every vsync from first use and never removes itself** (228, 245), so once created it wakes the main thread every vsync for the process lifetime.
  - [C] It imports `me.vkryl.core.reference.ReferenceList` (line 17). The Telegram X lineage this suggests is a guess [U].
- **[C]** Rates used: outgoing-state icons 15 fps (ChatMessageCell.java:21232), animated emoji 15 (AnimatedEmojiDrawable.java:1242), caret 2 (EditTextBoldCursor.java:1148), status drawables 30 (StatusDrawable.java:23), capsule blob 60 (CapsuleBlobDrawable.java:348), gift particles 15 (GiftSheet.java:2450).
  - Lottie and GIF use their source fps, halved when fps is limited (RLottieDrawable.java:1399-1406; AnimatedFileDrawable.java:1408).
- **[C]** AnimatedTextView re-lays out only the changed parts once, then animates by drawing. The default is 320 ms EASE_OUT_QUINT (AnimatedTextView.java:164-165).
- **[C]** TextMessageEnterTransition:
  - A 250 ms linear animator with hand-applied easing per axis: Y on the chat curve, X through EASE_OUT_QUINT then EASE_OUT, alpha within the first 40 % (TextMessageEnterTransition.java:381-390, 466-475).
  - It is skipped for multi-block text or more than 10 lines (129), and on LOW tier (ChatListItemAnimator.java:361). It adds a HIGH-only part (410).
  - Its bitmap path is never enabled (83, 339).
- **[C]** StableAnimator advances a fixed number of steps, duration/screenRefreshTime, so dropped frames lengthen it (StableAnimator.java:64).

## 9. Media pipeline

- **[C]** Lottie:
  - Frames render on two fixed pools: 4 threads named Lottie-N and 2 named LottieLow-N for limited-fps drawables (RLottieDrawable.java:132-133, 936-939).
  - Default size is min(512, dp(170.6)) (ImageLoader.java:913-914).
  - Filters of 90x90 dp or smaller without `nolimit` cap the size at 160 px and set limitFps (931-935). limitFps is dropped when the source is under 60 fps (RLottieDrawable.java:544-546, 561-562).
  - The precache rule is in §0.8.
- **[C]** Image memory cache size: min(30 MB if memoryClass >= 192 else 15 MB, memoryClass/7 MB), split 80 % common and 20 % small (ImageLoader.java:2075-2085).
- **[C]** Disk-cache decode runs on a single-thread priority executor that a latch can pause (DispatchQueuePriority.java:13-41, 74-86; ImageLoader.java:129). Thumbnail generation runs at MIN priority (2073).
- **[C]** HwEmojis pauses the decode queue while the custom-emoji reaction window animates. It collects views whose invalidations it swallows, all of them on non-HIGH tiers, and invalidates them once at the end (HwEmojis.java:34-37, 66-105).
- **[C]** Cross-fade is 150 ms (ImageReceiver.java:226). The background-thread path uses real dt, starting at 16 ms with a 30 ms cap above 60 Hz. **The UI-thread path adds a fixed 16 ms per draw** (1842-1852).
  - [CALC] At 90 Hz the fade finishes in about 2/3 of the nominal time, about 100 ms. DialogCell's archive and reveal states also step by fixed 16 ms per draw (DialogCell.java:4923-4952).
- **[C]** Bitmap recycling is deferred 36 ms and then runs on globalQueue (AndroidUtilities.java:831-862).
- **[C]** Emoji picker rows draw on 2 background threads into bitmaps (DrawingInBackgroundThreadDrawable.java:20, 98).

## 10. Layers, caches, blur

- **[C]** There are 77 LAYER_TYPE_HARDWARE uses in 43 files. They are mostly switched on for an animation or gesture and off afterwards: ABL swipe, BottomSheet, attach sheet, profile list, Dialogs/Topics slide, PhotoViewer.
- **[C]** CachedStaticLayout records a StaticLayout into a RenderNode on API 29+ and replays it. Only SpoilerEffect uses it (CachedStaticLayout.java:32-54; grep).
- **[C]** Bubble backgrounds are pre-rendered dp(50) x dp(40) ARGB bitmaps used as nine-patches. They are cached in a 4x4 table by neighbour and selection state, and the shadow gets 4 variants (MessageDrawable.java:70, 80, 371-407).
- **[C]** Chat bar blur:
  - **Bitmap path:** below SDK 31, or with `useNewBlur` off (default on, SharedConfig.java:676). The strip under the bars is drawn into a 1/12-scale bitmap from a reuse pool **on the UI thread**. Only the stack blur runs on the BlurQueue thread. Radius = max(6, max(w,h)/180) x 2.5 x user factor. A scroll offset modulo 24 px fixes resampling shimmer (SizeNotifierFrameLayout.java:63-65, 117-119, 610-716).
  - **RenderNode path:** RenderEffect blur chained with a saturation-2 colour filter, at the tier scale and radius of §4.3 (980-1035).

## 11. Lists

- **[C]** RecyclerView is vendored in 39 files. The view cache stays at the stock 2 (RecyclerView.java:5931). The recycled pool's default max scrap is **20**; stock is 5 [L, from memory] (5655).
- **[C]** No list calls setItemViewCacheSize, and no prefetch tuning was found.
- **[C]** The chat list uses GridLayoutManagerFixed with 1000 spans (CA:6553).
- **[C]** ChatListItemAnimator:
  - Moves and changes take 250 ms on the cubic-bezier (0.19919, 0.01064, 0.27921, 0.91025). The stock 120 ms add/remove durations stay (ChatListItemAnimator.java:46-47, 1629-1637; RecyclerView.java:12796-12798).
  - Change animations are off and moves are always created (73-75).
  - A side ValueAnimator of remove+move = 370 ms only ticks visible-part updates (112-121).
  - Alpha-enter adds a 0.9 scale-in and a start delay of (1 - bottom/listHeight) x 100 ms (1421, 1495-1512).
  - Move duration is x1.9 on the vaporize path (984). The greeting sticker uses 350 ms Overshoot (1507-1509).
- **[C]** Dialogs list:
  - The item animator runs 180 ms (DialogsItemAnimator.java:46-47).
  - Animated updates install the animator. Non-animated updates remove it and coalesce within 36 ms (DialogsActivity.java:471-489).
- **[C]** Long jumps animate translationY of the old and new screens. Duration is (distance/height + 1) x 200 ms clamped to 300..1300 ms with EASE_OUT_QUINT. With shared rows it is 600 ms for chat, and 150 ms EASE_OUT for dialogs (RecyclerAnimationScrollHelper.java:320-347).
- **[C]** Chat paging loads 50 messages when 25 rows remain while scrolling (5 otherwise) (CA:13888-13915).
- **[C]** Message text layout:
  - Split into blocks of 10 lines, 5 with many emoji (MessageObject.java:1821-1822).
  - Rebuilt only if the min width changes by more than dp(52) or density or font size changes (6833).
  - HIGH_QUALITY break strategy (8639).
- **[C]** ChatMessageCell (29,584 lines) extends a ViewGroup base class and adds no child views. The one addView call registers a callback (ChatMessageCell.java:263, 10834; BaseCell.java:27). onMeasure returns stored heights and re-binds only when the layout is stale (13676-13697).
- **[C]** RecyclerListView returns false from hasOverlappingRendering (2557-2559).

## 12. Refresh rate and instrumentation

- **[C]** screenRefreshRate and the max supported rate are read from the Display (AndroidUtilities.java:2766-2780).
- **[C]** The max rate is requested on overlay windows (PiP, avatar preview, reaction overlays, theme editor), on several activities, and on LaunchActivity's window **only in onConfigurationChanged** (LaunchActivity.java:7125; callers via grep).
- **[C]** RefreshRateController would drop to 60 Hz at 55 fps or below and return at 58.5 or above. Its window is 1.8 s, with at least 3 s between switches and a 240-frame ring. Its creation is commented out (RefreshRateController.java:48-62; LaunchActivity.java:395, 854-856).
- **[C]** FrameMetricsOverlayView runs on its own HandlerThread with an EWMA alpha of 0.05 and repaints every 300 ms. It is attached only when the hidden `frameMetricsEnabled` setting is on, which defaults to false (FrameMetricsOverlayView.java:170, 178, 224; LaunchActivity.java:860-870; SharedConfig.java:622).
- **[C]** Debug only: a StrictMode VM policy for leaked closables (LaunchActivity.java:401-406) and a main-thread assertion in NotificationCenter (NC:657-661).

## 13. Interpolators and durations

- **[C]** Curves:
  - DEFAULT (0.25, 0.1, 0.25, 1); EASE_OUT (0, 0, 0.58, 1); EASE_OUT_QUINT (0.23, 1, 0.32, 1); EASE_IN (0.42, 0, 1, 1); EASE_BOTH (0.42, 0, 0.58, 1); EASE_OUT_BACK (0.34, 1.56, 0.64, 1).
  - Emphasized is a two-segment path. EmphasizedDecelerate (0.05, 0.7, 0.1, 1), EmphasizedAccelerate (0.3, 0, 0.8, 0.15), StandardDecelerate (0, 0, 0, 1) (CubicBezierInterpolator.java:11-22).
  - Chat curve (0.19919, 0.01064, 0.27921, 0.91025) (ChatListItemAnimator.java:47).
- **[C] Durations in ms:**
  - Page push/pop 150 (preview 190). Swipe commit 200 x rem/w (min 50; predictive min 380). Swipe cancel 320 x x/w (min 120; predictive 320). Tablet layer 200.
  - Profile open 180/250. Profile pull 250/factor.
  - Sheet open 400 (right variant 250). Sheet dismiss 250 (button 180, call 330, right 200). Sheet snap-back 250 x ty/0.8 cm. Sheet side cancel 220, commit 320.
  - Popup 150 + 16n, items 180, dismiss 150. Submenu 300 x factor.
  - Keyboard 250. Chat item move 250 and add/remove 120. Chat scrim 320. ItemOptions 380/150.
  - Pull to next chat 300. Topics to chat 150.
  - Tab pager 150..600, tab tap 540.
  - Image crossfade 150. AnimatedFloat 200. AnimatedTextView 320. Message enter 250.
  - Photo open/close 200, animateTo 250. Long jump 300..1300.
  - Dialogs item 180. Dialogs update coalescing 36.

## 13a. What the user's AVERAGE-tier phone gets by default [CALC from the code above]

The phone is assumed AVERAGE (see §4.1). With default settings:
- **Effects off:** MEDIUM preset, so no chat blur, no wallpaper motion, no spoiler particles and no chat-scale effect.
- **Dialogs parallax:** translation only, with no layers.
- **Topics slide:** layer plus scale.
- **Lottie:** precached to disk.
- **Limits:** 50 emoji per text; first chat page of 15.
- **Image decode:** RGB_565 unless memoryClass >= 192.
- **Scrolling:** no heavy-ops pause on chat scroll (LOW only); spoilers still stop.
- **Emoji drawing:** background emoji drawing pauses on 512.
- **HwEmojis:** grabs views.
- **Message enter:** on, without the HIGH bubble morph.

## 14. Web and third-party

- **[C]** The rumitvn "Telegram Android Internals Handbook" analyses tag release-11.4.2-5469 at commit fb2e5451. Its chapters cover the frame budget/AnimatedFloat, ChatMessageCell, RecyclerListView, emoji, rlottie, and ImageLoader (https://github.com/rumitvn/telegram-book, fetched).
  - **[U]** Its native-code claims (rlottie 60 fps / 600-frame limits, frame lead times) cannot be checked because there is no jni/ in the clone.
- **[C]** On Hacker News, user grishka (2024-08-03) says that ChatMessageCell hand-draws all non-service messages on a canvas for performance and handles input by hand (https://news.ycombinator.com/item?id=41148996, fetched). They describe themselves as a former developer; that was not re-checked.
- **[C]** https://bugs.telegram.org/c/59829 reports main-list scrolling stutter on 120 Hz phones in 12.5.0 (65732) on Android 16. Users say it is worse on custom ROMs and long lists, that Telegram X scrolls smoothly, and the refresh overlay dips to 60-90 Hz. There is no staff reply (fetched).
- **[L]** https://bugs.telegram.org/c/15403 (8.6.0): a user describes device classing that gates the blurred header and other effects. A 2022-06-05 reply, from a poster not identified as staff, calls it intended (fetched).
- **[U]** These were not re-fetched:
  - Telegram blog and channel animation release notes: 7.1.3, 7.2.0, 7.3.0, 7.8.0, 8.2.0, 8.5.0, 9.5.0 power saving ("200+ device models"), 10.5.0.
  - Contest briefs: Android 2021 r2, with Galaxy J7 Prime and Redmi Note 4 test devices; Calls 2023.
  - The DEV.to opinion post.
- None of these give frame-time numbers.

## 15. Compose facts for the mapping step (official docs)

- **[C]** compose-animation versions: latest stable 1.12.1 (2026-09-09), 1.12.0 (2026-08-12), 1.11.0 (2026-04-22), 1.10.0 (2025-12-03), 1.7.0 (2024-09-04). Beta 1.13.0-beta01 (2026-10-07). Shared transition APIs became stable in 1.10 (developer.android.com/jetpack/androidx/releases/compose-animation, fetched).
- **[L]** Compose spring constants (from memory, stable API):
  - Stiffness: High 10000, Medium 1500, MediumLow 400, Low 200, VeryLow 50.
  - Damping: HighBouncy 0.2, MediumBouncy 0.5, LowBouncy 0.75, NoBouncy 1.0.
  - `spring()` defaults to damping 1.0 and stiffness 1500. dynamicanimation SpringForce defaults to damping 0.5 and stiffness 1500.
  - Both use the same damped-oscillator parameters, so Telegram's (damping, stiffness) pairs translate directly. Identical motion between the two integrators is untested.
- **[L]** Animatable and animate*AsState keep velocity when the target changes; tween restarts from the current value. AnimatedFloat keeps value only (see §8).
- **[L]** Reading animated state inside graphicsLayer{} or a draw lambda re-runs only the draw phase.
- **[L]** CompositingStrategy.Auto goes offscreen when alpha < 1 or a RenderEffect is set; ModulateAlpha avoids the buffer. This is the Compose counterpart of hasOverlappingRendering (§2.1).
- **[L]** Modifier.blur works on API 31+ only.
- **[L]** These are from pages the gatherer fetched:
  - animateItem() replaced animateItemPlacement in 1.7.0.
  - Navigation 3: NavDisplay predictivePopTransitionSpec; stable 1.0.0 (2025-11-19), 1.2.0 (2026-09-23).
  - PredictiveBackHandler and NavigationBackHandler give progress 0..1.
  - Adaptive refresh rate (Android 15 QPR1+) and Modifier.preferredFrameRate in Compose 1.9+.
  - Macrobenchmark FrameTimingMetric (frameOverrunMs, frameDurationCpuMs; P50 to P99; API 31+).
- **[U]** The default of pausable composition in lazy prefetch in 1.12.1 (release notes conflict). The AGP 9.2 / compileSdk 37 requirement of Compose 1.12 (search snippet only).

## 16. Dropped or not established

- The exteraGram LNavigation spring navigation and every exteraGram setting: source not on disk.
- "IME insets animation on 90 Hz+": dead code (§0.6).
- "Right-hand sheet opens with DEFAULT": overwritten (§0.4).
- "Sheet dismiss 180 ms": only the button path (§0.5).
- "Blur node re-recorded only when invalidated" as a general rule: only in chat and dialogs (§0.7).
- Any measured frame time. Nothing was built or run, and the repo has no benchmarks.
- res/ values: layer_shadow, header_shadow, XML anim/dimens.
- The thread for MessageObject builds on the network path. Only the cache path (storageQueue to stageQueue) was traced.
- The bodies of five needDelayOpenAnimation overrides. ChatMessageCell draw internals, ChatActivityEnterView panels, StoryViewer, SharedMediaLayout, EmojiView, DialogCell beyond the step code, and the blur3 package.
