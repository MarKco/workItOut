#!/usr/bin/env python3
"""Builds every WorkItOut icon asset from one geometry description.

    python3 design/build_icon.py

Writes:
  design/ic_launcher_mark.svg        the mark alone, transparent, 512x512 (source of truth)
  design/ic_launcher_playstore.svg   the mark on white, laid out like the adaptive icon
  app/src/main/ic_launcher-playstore.png               512x512 for the Play Console (needs rsvg-convert)
  app/src/main/res/drawable/ic_launcher_foreground.xml  adaptive icon foreground (108dp)
  app/src/main/res/drawable/ic_launcher_monochrome.xml  themed icon layer (Android 13+)
  app/src/main/res/drawable/ic_launcher_for_topbar.xml  the mark for the app bar

The drawing was reconstructed from the old raster icon (centre 256,256; hole r=151; inner band
151-197; outer band 197-249; a 60px channel at 45 degrees cuts the inner band; an "L" hand and an
arrow with round caps). Edit the constants below to change it.
"""
import math
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"

C = 256.0                      # centre of the 512 canvas
R_HOLE, R_MID, R_OUT = 151.0, 197.0, 249.0
CHANNEL_HALF = 30.0            # half width of the notch cut in the inner band, at 45 degrees

OUTER_DARK, OUTER_LIGHT = "#FDA506", "#FEC254"
INNER_DARK, INNER_LIGHT = "#FA6101", "#FD831E"
HAND = "#FE6B00"
ARROW = "#FC7506"

HAND_WIDTH = 15.0
HAND_POINTS = [(256, 163), (256, 258), (336, 258)]
ARROW_WIDTH = 7.0
ARROW_SHAFT = [(340, 172), (381, 131)]
ARROW_HEAD = [(347, 133), (381, 131), (378, 165)]

# The dark/light split is a straight line on each side, measured on the old icon.
SPLIT_UL = ((130.4, 156.9), (69.9, 96.7))     # upper left
SPLIT_LR = ((324.1, 400.8), (376.3, 469.4))   # lower right

# Adaptive icon: 108dp canvas, ring radius in dp (visible mask is a 72dp circle, safe zone 66dp).
ADAPTIVE_CANVAS = 108.0
ADAPTIVE_RING_RADIUS = 35.0


def f(n):
    return f"{n:.2f}".rstrip("0").rstrip(".")


def pt(p):
    return f"{f(p[0])},{f(p[1])}"


def line_circle(a, b, r, near):
    """Intersection of the line a-b with the circle of radius r around the centre, closest to `near`."""
    dx, dy = b[0] - a[0], b[1] - a[1]
    fx, fy = a[0] - C, a[1] - C
    qa = dx * dx + dy * dy
    qb = 2 * (fx * dx + fy * dy)
    qc = fx * fx + fy * fy - r * r
    disc = math.sqrt(qb * qb - 4 * qa * qc)
    hits = [(a[0] + t * dx, a[1] + t * dy) for t in ((-qb - disc) / (2 * qa), (-qb + disc) / (2 * qa))]
    return min(hits, key=lambda h: (h[0] - near[0]) ** 2 + (h[1] - near[1]) ** 2)


def on_circle(angle_deg, r):
    a = math.radians(angle_deg)
    return (C + r * math.cos(a), C - r * math.sin(a))


def sector(p_out_a, p_out_b, p_in_b, p_in_a, r_out, r_in, large):
    """Annular piece: outer arc a->b counter-clockwise on screen, straight to the inner arc, back."""
    return (
        f"M{pt(p_out_a)}A{f(r_out)},{f(r_out)} 0 {large} 0 {pt(p_out_b)}"
        f"L{pt(p_in_b)}A{f(r_in)},{f(r_in)} 0 {large} 1 {pt(p_in_a)}Z"
    )


def build_paths():
    """Returns dict name -> (path data, colour)."""
    near_ul = on_circle(140, R_OUT)
    near_lr = on_circle(298, R_OUT)
    ul = {r: line_circle(*SPLIT_UL, r, on_circle(140, r)) for r in (R_HOLE, R_MID, R_OUT)}
    lr = {r: line_circle(*SPLIT_LR, r, on_circle(298, r)) for r in (R_HOLE, R_MID, R_OUT)}
    del near_ul, near_lr

    paths = {}
    # Dark half runs counter-clockwise from the upper-left split to the lower-right split (< 180 deg).
    paths["outer_dark"] = (sector(ul[R_OUT], lr[R_OUT], lr[R_MID], ul[R_MID], R_OUT, R_MID, 0), OUTER_DARK)
    # Light half: the rest of the ring, more than 180 deg.
    paths["outer_light"] = (sector(lr[R_OUT], ul[R_OUT], ul[R_MID], lr[R_MID], R_OUT, R_MID, 1), OUTER_LIGHT)
    paths["inner_dark"] = (sector(ul[R_MID], lr[R_MID], lr[R_HOLE], ul[R_HOLE], R_MID, R_HOLE, 0), INNER_DARK)

    # Inner light half, minus the channel along the 45 degree axis (edges parallel to it).
    c = math.sqrt(0.5)
    axis, normal = (c, -c), (c, c)

    def edge(r, side):
        t = math.sqrt(r * r - CHANNEL_HALF ** 2)
        return (C + t * axis[0] + side * CHANNEL_HALF * normal[0], C + t * axis[1] + side * CHANNEL_HALF * normal[1])

    lr_out, lr_in = edge(R_MID, +1), edge(R_HOLE, +1)   # edge towards the lower right
    ul_out, ul_in = edge(R_MID, -1), edge(R_HOLE, -1)   # edge towards the upper left
    piece_a = sector(lr[R_MID], lr_out, lr_in, lr[R_HOLE], R_MID, R_HOLE, 0)
    piece_b = sector(ul_out, ul[R_MID], ul[R_HOLE], ul_in, R_MID, R_HOLE, 0)
    paths["inner_light"] = (piece_a + piece_b, INNER_LIGHT)
    return paths


def polyline(points):
    return "M" + "L".join(pt(p) for p in points)


def mark_svg_body(indent="  "):
    lines = []
    for name, (d, colour) in build_paths().items():
        lines.append(f'{indent}<path id="{name}" fill="{colour}" d="{d}"/>')
    lines.append(
        f'{indent}<path id="hand" fill="none" stroke="{HAND}" stroke-width="{f(HAND_WIDTH)}" '
        f'stroke-linecap="round" stroke-linejoin="round" d="{polyline(HAND_POINTS)}"/>'
    )
    lines.append(
        f'{indent}<path id="arrow" fill="none" stroke="{ARROW}" stroke-width="{f(ARROW_WIDTH)}" '
        f'stroke-linecap="round" stroke-linejoin="round" d="{polyline(ARROW_SHAFT)}{polyline(ARROW_HEAD)}"/>'
    )
    return "\n".join(lines)


def write_svgs():
    mark = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">\n{mark_svg_body()}\n</svg>\n'
    (ROOT / "design/ic_launcher_mark.svg").write_text(mark)

    scale = (ADAPTIVE_RING_RADIUS / ADAPTIVE_CANVAS * 512) / R_OUT
    offset = C - C * scale
    store = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">\n'
        '  <rect width="512" height="512" fill="#FFFFFF"/>\n'
        f'  <g transform="translate({f(offset)} {f(offset)}) scale({scale:.5f})">\n'
        f'{mark_svg_body("    ")}\n  </g>\n</svg>\n'
    )
    (ROOT / "design/ic_launcher_playstore.svg").write_text(store)


# --- Android vector drawables ------------------------------------------------------------------

def vector_xml(width_dp, viewport, group_attrs, mono=False):
    out = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{f(width_dp)}dp"',
        f'    android:height="{f(width_dp)}dp"',
        f'    android:viewportWidth="{f(viewport)}"',
        f'    android:viewportHeight="{f(viewport)}">',
    ]
    indent = "    "
    if group_attrs:
        out.append("    <group")
        out += [f'        android:{k}="{v}"' for k, v in group_attrs.items()]
        out[-1] += ">"
        indent = "        "
    for name, (d, colour) in build_paths().items():
        fill = "#000000" if mono else colour
        alpha = ""
        if mono and name.startswith("outer"):
            alpha = '\n' + indent + '    android:fillAlpha="0.55"'
        out.append(f'{indent}<path\n{indent}    android:fillColor="{fill}"{alpha}\n{indent}    android:pathData="{d}" />')
    for colour, width, d in (
        (HAND, HAND_WIDTH, polyline(HAND_POINTS)),
        (ARROW, ARROW_WIDTH, polyline(ARROW_SHAFT) + polyline(ARROW_HEAD)),
    ):
        stroke = "#000000" if mono else colour
        out.append(
            f'{indent}<path\n{indent}    android:strokeColor="{stroke}"\n{indent}    android:strokeWidth="{f(width)}"\n'
            f'{indent}    android:strokeLineCap="round"\n{indent}    android:strokeLineJoin="round"\n'
            f'{indent}    android:pathData="{d}" />'
        )
    if group_attrs:
        out.append("    </group>")
    out.append("</vector>")
    return "\n".join(out) + "\n"


def write_android():
    scale = ADAPTIVE_RING_RADIUS / R_OUT
    shift = ADAPTIVE_CANVAS / 2 - C
    group = {
        "scaleX": f"{scale:.5f}", "scaleY": f"{scale:.5f}",
        "pivotX": f(C), "pivotY": f(C),
        "translateX": f(shift), "translateY": f(shift),
    }
    drawable = RES / "drawable"
    (drawable / "ic_launcher_foreground.xml").write_text(vector_xml(ADAPTIVE_CANVAS, ADAPTIVE_CANVAS, group))
    (drawable / "ic_launcher_monochrome.xml").write_text(vector_xml(ADAPTIVE_CANVAS, ADAPTIVE_CANVAS, group, mono=True))
    (drawable / "ic_launcher_for_topbar.xml").write_text(vector_xml(32, 512, None))


def write_png():
    out = ROOT / "app/src/main/ic_launcher-playstore.png"
    subprocess.run(
        ["rsvg-convert", "-w", "512", "-h", "512", str(ROOT / "design/ic_launcher_playstore.svg"), "-o", str(out)],
        check=True,
    )
    subprocess.run(
        ["rsvg-convert", "-w", "512", "-h", "512", str(ROOT / "design/ic_launcher_mark.svg"), "-o", "/dev/stdout"],
        check=True, stdout=subprocess.DEVNULL,
    )


if __name__ == "__main__":
    (ROOT / "design").mkdir(exist_ok=True)
    write_svgs()
    write_android()
    write_png()
    print("done")
