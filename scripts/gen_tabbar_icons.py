#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 tabBar 图标（81×81 PNG，2 倍图下 40.5pt）。

小程序的 tabBar 只接受图片，不能内联 SVG，也不便用字体图标，
所以用 Pillow 画三组极简线性图标：书册（图鉴）、衣袍（搭配）、人像（我的）。
未选中用墨色 #918780，选中用朱砂 #A6322A。

用法：python scripts/gen_tabbar_icons.py
"""
import os

from PIL import Image, ImageDraw

OUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "miniapp", "src", "static", "tabbar",
)
SIZE = 81
SS = 4  # 超采样倍数，缩小后描边才平滑
INK = (145, 135, 128)
ACCENT = (166, 50, 42)


def canvas():
    img = Image.new("RGBA", (SIZE * SS, SIZE * SS), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def stroke(draw, color, width=5):
    return max(1, width * SS // 4)


def draw_book(draw, color, w):
    """书册：一本翻开的线装书。"""
    m = 14 * SS
    right = SIZE * SS - m
    top = 20 * SS
    bottom = SIZE * SS - 20 * SS
    draw.rounded_rectangle([m, top, right, bottom], radius=4 * SS, outline=color, width=w)
    mid = (m + right) // 2
    draw.line([mid, top + 2 * SS, mid, bottom - 2 * SS], fill=color, width=w)
    for i in range(3):
        y = top + (11 + i * 6) * SS
        draw.line([m + 4 * SS, y, mid - 4 * SS, y], fill=color, width=max(1, w - 1))
        draw.line([mid + 4 * SS, y, right - 4 * SS, y], fill=color, width=max(1, w - 1))


def draw_robe(draw, color, w):
    """衣袍：交领宽袖的成衣轮廓。"""
    cx = SIZE * SS // 2
    top = 16 * SS
    # 肩线与袖
    draw.line([cx - 16 * SS, top + 8 * SS, cx - 26 * SS, top + 20 * SS], fill=color, width=w)
    draw.line([cx + 16 * SS, top + 8 * SS, cx + 26 * SS, top + 20 * SS], fill=color, width=w)
    draw.line([cx - 26 * SS, top + 20 * SS, cx - 26 * SS, top + 30 * SS], fill=color, width=w)
    draw.line([cx + 26 * SS, top + 20 * SS, cx + 26 * SS, top + 30 * SS], fill=color, width=w)
    # 躯干
    draw.line([cx - 16 * SS, top + 8 * SS, cx - 12 * SS, SIZE * SS - 16 * SS], fill=color, width=w)
    draw.line([cx + 16 * SS, top + 8 * SS, cx + 12 * SS, SIZE * SS - 16 * SS], fill=color, width=w)
    draw.line(
        [cx - 12 * SS, SIZE * SS - 16 * SS, cx + 12 * SS, SIZE * SS - 16 * SS], fill=color, width=w
    )
    # 交领
    draw.line([cx - 8 * SS, top + 8 * SS, cx, top + 22 * SS], fill=color, width=w)
    draw.line([cx + 8 * SS, top + 8 * SS, cx, top + 22 * SS], fill=color, width=w)


def draw_user(draw, color, w):
    """人像：头 + 肩。"""
    cx = SIZE * SS // 2
    head_r = 11 * SS
    head_cy = 30 * SS
    draw.ellipse(
        [cx - head_r, head_cy - head_r, cx + head_r, head_cy + head_r], outline=color, width=w
    )
    draw.arc(
        [cx - 22 * SS, head_cy + 6 * SS, cx + 22 * SS, head_cy + 52 * SS],
        start=200,
        end=340,
        fill=color,
        width=w,
    )


ICONS = {"book": draw_book, "robe": draw_robe, "user": draw_user}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, fn in ICONS.items():
        for suffix, color in (("", INK), ("-active", ACCENT)):
            img, draw = canvas()
            fn(draw, color, stroke(draw, color))
            img = img.resize((SIZE, SIZE), Image.LANCZOS)
            path = os.path.join(OUT_DIR, f"{name}{suffix}.png")
            img.save(path, "PNG", optimize=True)
            print(f"  + {os.path.relpath(path)}")


if __name__ == "__main__":
    main()
