"""Patches a generated Everything project's catalog into one arm of the media A/B (media.md 4.5, 4.6).

    python -I media-arms.py <generated project> --arm B [--decodes 2] [--pool 3] [--beyond 1] [--texture] [--frame-rate-off]
    python -I media-arms.py <generated project> --check      # every arm applies, on copies in memory; nothing is written

Never run on the template or a worktree: only on a project generated for the session (it refuses a directory that
holds `features/` or `generator/`). Each patch must match exactly once, or the arm stops with what did not match,
so a template change that moves a line is caught before a phone session, not in its numbers.

Image arms (media.md 4.5):
  N     SubcomposeAsyncImage with a spinner while loading, crossfade, the height from the loaded picture, Coil's
        default singleton, no prefetch: what an app without the template's rules does.
  A     AppNetworkImage as on staging d429cbf (model = url), a loader with Coil's defaults and only the client, no prefetch.
  B     the feature as shipped (no patch).
  B-P   B without prefetch.
  B+F   B with fadeIn = true on every image of the scenes.
  B-S   every image of the scenes drawn as the skeleton a redacted image draws (the trace's negative control).
  B-HW0 B with software bitmaps (the trace's positive control).
Options for any arm: --decodes (the loader's decodes at once), --pool, --beyond, --texture, --frame-rate-off (the
video arms of 4.6: V2 = --pool 2, V3b0 = --beyond 0, VT = --texture, VF = --frame-rate-off).
"""
import argparse
import pathlib
import re
import sys

ARMS = ("N", "A", "B", "B-P", "B+F", "B-S", "B-HW0")


class ArmError(Exception):
    pass


class Project:
    """The generated files an arm touches, read once, written back only when every patch matched."""

    def __init__(self, root):
        self.root = pathlib.Path(root)
        if (self.root / "features").is_dir() or (self.root / "generator").is_dir():
            raise ArmError(f"{root} looks like the template or the generator repository, not a generated project")
        self.texts = {}

    def find(self, pattern):
        matches = sorted(self.root.glob(pattern))
        if len(matches) != 1:
            raise ArmError(f"expected one {pattern}, found {len(matches)}")
        return matches[0]

    def text(self, path):
        if path not in self.texts:
            self.texts[path] = path.read_text(encoding="utf-8")
        return self.texts[path]

    def replace(self, pattern, old, new, count=1, regex=False):
        path = self.find(pattern)
        text = self.text(path)
        found = len(re.findall(old, text, re.S)) if regex else text.count(old)
        if found != count:
            raise ArmError(f"{path.relative_to(self.root)}: expected {count} of {old!r}, found {found}")
        self.texts[path] = re.sub(old, lambda _: new, text, flags=re.S) if regex else text.replace(old, new)

    def write(self):
        for path, text in self.texts.items():
            path.write_text(text, encoding="utf-8", newline="\n")


FEED = "catalog/src/main/kotlin/**/catalog/media/photos/FeedScreen.kt"
SHOP = "catalog/src/main/kotlin/**/catalog/media/photos/ShopScreen.kt"
ACTIVITY = "catalog/src/main/kotlin/**/catalog/CatalogActivity.kt"
IMAGE = "core/image/src/main/kotlin/**/core/image/AppNetworkImage.kt"
LOADER = "core/image/src/main/kotlin/**/core/image/AppImageLoader.kt"
VIDEO_DEFAULTS = "core/video/src/main/kotlin/**/core/video/VideoDefaults.kt"
APP_VIDEO = "core/video/src/main/kotlin/**/core/video/AppVideo.kt"
ENGINE = "core/video/src/main/kotlin/**/core/video/engine/PreloadingEngine.kt"

_REQUEST = re.compile(r"    val context = LocalPlatformContext\.current\n.*?\n    AsyncImage\(\n        model = request,", re.S)
#: The request and the whole AsyncImage call that draws it, to the end of the function.
_WHOLE_REQUEST = re.compile(r"    val context = LocalPlatformContext\.current\n.*?\n    AsyncImage\(\n.*?\n    \)\n}\n", re.S)


def no_prefetch(project):
    project.replace(FEED, r"\n    PrefetchImages\(list\) \{[^\n]*\}", "", regex=True)
    project.replace(SHOP, r"\n    PrefetchImages\(grid\) \{[^\n]*\}", "", regex=True)


def default_loader(project, with_client):
    """Coil's default singleton (N), or a loader with Coil's defaults and only the catalog's client (A)."""
    old = ("        SingletonImageLoader.setSafe { context ->\n"
           "            appImageLoader(context, callFactory = { CatalogMediaClient }, events = MediaPerfLog.events(intent))\n"
           "        }\n")
    if with_client:
        new = ("        SingletonImageLoader.setSafe { context ->\n"
               "            coil3.ImageLoader.Builder(context)\n"
               "                .components { add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { CatalogMediaClient })) }\n"
               "                .apply { MediaPerfLog.events(intent)?.let(::eventListenerFactory) }\n"
               "                .build()\n"
               "        }\n")
    else:
        new = ("        SingletonImageLoader.setSafe { context ->\n"
               "            coil3.ImageLoader.Builder(context)\n"
               "                .apply { MediaPerfLog.events(intent)?.let(::eventListenerFactory) }\n"
               "                .build()\n"
               "        }\n")
    project.replace(ACTIVITY, old, new)


def todays_image(project):
    """AppNetworkImage as on staging: the url itself is the model, so Coil builds the request."""
    project.replace(IMAGE, _REQUEST.pattern, "    AsyncImage(\n        model = url,", regex=True)


def naive_image(project):
    """A subcomposing image with a spinner and a crossfade, whose height comes from the loaded picture."""
    base = re.search(r"^package (\S+)\.core\.image$", project.text(project.find(IMAGE)), re.M).group(1)
    project.replace(
        IMAGE, _WHOLE_REQUEST.pattern,
        "    coil3.compose.SubcomposeAsyncImage(\n"
        "        model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),\n"
        "        contentDescription = contentDescription,\n"
        "        modifier = modifier,\n"
        f"        loading = {{ {base}.core.designsystem.component.feedback.AppActivityIndicator() }},\n"
        "        contentScale = contentScale,\n"
        "    )\n}\n",
        regex=True,
    )
    project.replace(FEED, r"\n\s*\.aspectRatio\(post\.ratio\)", "", regex=True)


def skeleton_image(project):
    project.replace(
        IMAGE,
        "    if (url.isNullOrBlank()) {",
        "    AppSkeleton(modifier = modifier, shape = RectangleShape)\n    return\n    @Suppress(\"UNREACHABLE_CODE\")\n"
        "    if (url.isNullOrBlank()) {",
    )


def fade_everywhere(project):
    for scene, count in ((FEED, 3), (SHOP, 1)):
        project.replace(scene, "AppNetworkImage(", "AppNetworkImage(fadeIn = true, ", count=count)


def software_bitmaps(project):
    project.replace(LOADER, "    .allowHardware(true)\n", "    .allowHardware(false)\n")


def options(project, args):
    if args.decodes is not None:
        project.replace(LOADER, r"internal const val DECODES_AT_ONCE = \d+", f"internal const val DECODES_AT_ONCE = {args.decodes}",
                        regex=True)
    if args.pool is not None:
        project.replace(VIDEO_DEFAULTS, r"const val POOL_SIZE = \d+", f"const val POOL_SIZE = {args.pool}", regex=True)
    if args.beyond is not None:
        project.replace(VIDEO_DEFAULTS, r"const val BEYOND_PAGES = \d+", f"const val BEYOND_PAGES = {args.beyond}", regex=True)
    if args.texture:
        project.replace(APP_VIDEO, "SURFACE_TYPE_SURFACE_VIEW", "SURFACE_TYPE_TEXTURE_VIEW", count=2)
    if args.frame_rate_off:
        project.replace(ENGINE, "        repeatMode = Player.REPEAT_MODE_ONE\n",
                        "        repeatMode = Player.REPEAT_MODE_ONE\n"
                        "        videoChangeFrameRateStrategy = C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF\n")


def apply(project, arm, args):
    if arm == "N":
        naive_image(project)
        default_loader(project, with_client=False)
        no_prefetch(project)
    elif arm == "A":
        todays_image(project)
        default_loader(project, with_client=True)
        no_prefetch(project)
    elif arm == "B-P":
        no_prefetch(project)
    elif arm == "B+F":
        fade_everywhere(project)
    elif arm == "B-S":
        skeleton_image(project)
    elif arm == "B-HW0":
        software_bitmaps(project)
    options(project, args)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("project")
    parser.add_argument("--arm", choices=ARMS)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--decodes", type=int)
    parser.add_argument("--pool", type=int)
    parser.add_argument("--beyond", type=int)
    parser.add_argument("--texture", action="store_true")
    parser.add_argument("--frame-rate-off", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            every = argparse.Namespace(decodes=1, pool=2, beyond=0, texture=True, frame_rate_off=True)
            for arm in ARMS:
                apply(Project(args.project), arm, every)
                print(f"{arm}: applies")
            return 0
        if not args.arm:
            parser.error("--arm or --check")
        project = Project(args.project)
        apply(project, args.arm, args)
        project.write()
        print(f"{args.project}: arm {args.arm} applied to {len(project.texts)} files")
        return 0
    except ArmError as error:
        print(f"media-arms: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
