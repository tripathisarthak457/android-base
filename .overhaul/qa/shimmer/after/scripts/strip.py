"""Before/after frame strips with ffmpeg: frames side by side in rows, each with a label."""

import os
import subprocess
import tempfile

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
TMP = tempfile.mkdtemp(prefix="strip-", dir=os.environ.get("STRIP_TMP"))


def frame_png(source, index):
    """A PNG of frame [index] (decode order) of a recording, or the PNG itself."""
    if index is None:
        return source
    out = os.path.join(TMP, f"{os.path.basename(source)}-{index}.png")
    if not os.path.exists(out):
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", source, "-vf", f"select=eq(n\\,{index})",
                        "-fps_mode", "passthrough", "-frames:v", "1", out], check=True)
    return out


def escape(text):
    return text.replace("\\", "\\\\").replace(":", "\\:").replace("'", "’").replace(",", "\\,").replace("%", "\\%")


def compose(rows, out, cell_width, title=None, gap=8):
    """rows: [(row label, [cell]), ...]; cell: dict(src, index, crop=(x, y, w, h), label, lines, marks).

    lines: [(y, colour)] horizontal guides in source px; marks: [(x, y, colour)] 7 px squares.
    Every cell is scaled to cell_width; a row is as tall as its tallest cell.
    """
    inputs, filters, row_names = [], [], []
    n = 0
    for r, (row_label, cells) in enumerate(rows):
        names = []
        for c, cell in enumerate(cells):
            png = frame_png(cell["src"], cell.get("index"))
            inputs += ["-i", png]
            chain = []
            if cell.get("crop"):
                x, y, w, h = cell["crop"]
                chain.append(f"crop={w}:{h}:{x}:{y}")
                offset_x, offset_y = x, y
            else:
                offset_x = offset_y = 0
            for y, colour in cell.get("lines", []):
                chain.append(f"drawbox=x=0:y={y - offset_y - 1}:w=iw:h=3:color={colour}@0.9:t=fill")
            for x, y, colour in cell.get("marks", []):
                chain.append(f"drawbox=x={int(x - offset_x - 3)}:y={int(y - offset_y - 3)}:w=7:h=7:color={colour}@1:t=fill")
            chain.append(f"scale={cell_width}:-2:flags=lanczos")
            if cell.get("label"):
                chain.append("pad=iw:ih+34:0:0:color=0x303030")
                chain.append(f"drawtext=fontfile={FONT}:text='{escape(cell['label'])}':x=8:y=h-27:fontsize=20:"
                             f"fontcolor=white")
            chain.append(f"pad=iw+{gap}:ih:0:0:color=0x808080")
            filters.append(f"[{n}:v]" + ",".join(chain) + f"[c{n}]")
            names.append(f"[c{n}]")
            n += 1
        # hstack needs equal heights: every cell of a row has the same crop (or source) size.
        stack = "".join(names) + (f"hstack=inputs={len(names)}" if len(names) > 1 else "null")
        filters.append(f"{stack},pad=iw:ih+40:0:40:color=0x1F4E79,drawtext=fontfile={FONT_BOLD}:"
                       f"text='{escape(row_label)}':x=10:y=9:fontsize=22:fontcolor=white[r{r}]")
        row_names.append(f"[r{r}]")
    # Rows of different widths: pad to the widest; then stack with a gap.
    widths = [len(cells) * (cell_width + gap) for _, cells in rows]
    widest = max(widths)
    final = []
    for r, name in enumerate(row_names):
        filters.append(f"{name}pad={widest}:ih+{gap}:0:0:color=0x808080[q{r}]")
        final.append(f"[q{r}]")
    last = "".join(final) + (f"vstack=inputs={len(final)}" if len(final) > 1 else "null")
    if title:
        last += (f",pad=iw:ih+44:0:44:color=0x202020,drawtext=fontfile={FONT_BOLD}:text='{escape(title)}':x=12:y=11:"
                 f"fontsize=21:fontcolor=white")
    filters.append(last + "[out]")
    subprocess.run(["ffmpeg", "-v", "error", "-y", *inputs, "-filter_complex", ";".join(filters), "-map", "[out]",
                    "-frames:v", "1", out], check=True)
    return out
