#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen_art.py —— 《燕云十六声》古风时装搭配 · 程序化视觉素材生成器
================================================================
本机没有可用 AI 绘图模型，所有视觉素材由 Pillow + NumPy 程序化绘制。

【分层手法】每张竖版海报都是同一套「墨韵色卡」工序，自下而上：
  1. 宣纸底   以 #F7F2E9 为基，叠三层噪声：低频纸浆云斑 + 中频纸纤维 + 全分辨率细颗粒，
              用 ImageChops.add 做有符号微扰（±4 级），再用 0.4px 高斯柔化，避免数码脏感
  2. 色场     colorFamily 主色先降饱和（×0.42）再把明度压到 0.58~0.74，
              叠「径向柔光 + 斜向光带」两个场，smoothstep 羽化纸质边界，画面占比约 45%~60%
  3. 墨晕     2~4 团「多边形随机扰动 → 圆滑卷积 → 三遍箱式模糊」的墨块，
              在 1/4 分辨率累加亮度后双三次放大，低透明度叠加，模拟水墨在宣纸上的扩散
  4. 服装剪影 六大部位各一套轮廓（set 整袍 / top 广袖上衣 / bottom 罗裙 / headwear 冠笠 /
              accessory 披风云肩 / hairstyle 垂鬟发髻）。全部用三次贝塞尔采样成平滑多边形，
              半侧构造后镜像保证绝对对称（披风、发丝等分件单独构造）。
              主色深色变体填充 0.55 透明度；内层布料用浅色再叠 0.30；廓边 1.5px 更深描边（绣线感）；
              另加门襟 / 腰带 / 褶皱 / 云肩绦边等细节线，并叠一层大模糊墨影让剪影"长"在纸上
  5. 金线内框 距边 40px、1.5px #C9A063 细框，四角断开 12px 并加短饰缺口
  6. 竖排单字 右上角楷体单字（120px、主色加深、0.75 透明），下方接极小号竖排系列名
  7. 底部渐暗 底部 22% 高度从透明过渡到 rgba(16,14,12,0.55)，保证 App 卡片叠白字清晰

【技术要点】
  · 全流程在 4 倍超采样画布（4096×6144 / 6144×4096）绘制，最后 LANCZOS 缩到目标尺寸，杜绝毛边
  · 随机种子 = md5(文件 id) 前 8 位，同一条命令永远复现同一张图
  · 渐变 / 墨晕 / 阴影等平滑内容在 1/4 分辨率计算后双三次放大 —— 质量无损、内存与耗时友好
  · 输出 JPEG quality 88 / optimize=True / 4:2:0，单图 < 400KB

【用法】
  python scripts/gen_art.py                    # 全量生成
  python scripts/gen_art.py --only fashion-01  # 只重画一张
  python scripts/gen_art.py --only fashion     # 一个系列
  python scripts/gen_art.py --ss 1             # 低分辨率构图预览（不出正式文件）
"""

import argparse
import colorsys
import hashlib
import math
import os
import sys
import time
from dataclasses import dataclass

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

# ----------------------------------------------------------------------------
# 常量 / 路径 / 调色板
# ----------------------------------------------------------------------------
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_ROOT = os.path.join(ROOT, "backend", "upload", "images")

POSTER = (1024, 1536)
BANNER = (1536, 1024)
AVATAR = (256, 256)

FONT_DIR = r"C:\Windows\Fonts"
FONT_KAI = os.path.join(FONT_DIR, "simkai.ttf")     # 楷体（首选）
FONT_SONG = os.path.join(FONT_DIR, "simsun.ttc")
FONT_HEI = os.path.join(FONT_DIR, "simhei.ttf")

PAPER = "#F7F2E9"
PAPER_HI = "#FDFBF7"
PAPER_LO = "#EFE7D9"
INK = "#100E0C"
INK_SOFT = "#1B1815"
GOLD = "#C9A063"          # 金线
SEAL = "#A6322A"          # 朱砂

# 契约 1.3 色系（主色 / 浅色）
FAMILY = {
    "ink":      ("#1B1815", "#D9D0C6"),
    "cinnabar": ("#A6322A", "#F7E5E2"),
    "gold":     ("#B08A45", "#F6EEDF"),
    "moon":     ("#9AA6AF", "#F0F3F4"),
    "bamboo":   ("#56755A", "#E5EDE3"),
    "indigo":   ("#38485A", "#E3E8EE"),
    "purple":   ("#6B4159", "#EFE3EA"),
    "ochre":    ("#9A6238", "#F2E3D6"),
}

# 契约 3.2 —— 16 件时装，顺序即 fashion-01 … fashion-16
ITEMS = [
    dict(id="fi1001", n="fashion-01", glyph="玄", cat="set",       color="ink",      style="wuxia",  series="玄刃"),
    dict(id="fi1002", n="fashion-02", glyph="赤", cat="set",       color="cinnabar", style="wuxia",  series="赤霄"),
    dict(id="fi1003", n="fashion-03", glyph="金", cat="set",       color="gold",     style="court",  series="流金"),
    dict(id="fi1004", n="fashion-04", glyph="霜", cat="set",       color="moon",     style="wuxia",  series="霜刃"),
    dict(id="fi1005", n="fashion-05", glyph="松", cat="top",       color="bamboo",   style="scholar", series="松风"),
    dict(id="fi1006", n="fashion-06", glyph="幽", cat="top",       color="indigo",   style="court",  series="幽兰"),
    dict(id="fi1007", n="fashion-07", glyph="素", cat="top",       color="moon",     style="scholar", series="素练"),
    dict(id="fi1008", n="fashion-08", glyph="跋", cat="bottom",    color="ochre",    style="jianghu", series="行旅"),
    dict(id="fi1009", n="fashion-09", glyph="霓", cat="bottom",    color="bamboo",   style="xianxia", series="青霓"),
    dict(id="fi1010", n="fashion-10", glyph="笠", cat="headwear",  color="ink",      style="jianghu", series="寒山"),
    dict(id="fi1011", n="fashion-11", glyph="冠", cat="headwear",  color="gold",     style="court",  series="十六声"),
    dict(id="fi1012", n="fashion-12", glyph="绛", cat="accessory", color="purple",   style="wuxia",  series="绛雪"),
    dict(id="fi1013", n="fashion-13", glyph="牵", cat="accessory", color="indigo",   style="exotic", series="牵丝"),
    dict(id="fi1014", n="fashion-14", glyph="肩", cat="accessory", color="cinnabar", style="exotic", series="朱明"),
    dict(id="fi1015", n="fashion-15", glyph="髻", cat="hairstyle", color="moon",     style="xianxia", series="云髻"),
    dict(id="fi1016", n="fashion-16", glyph="丝", cat="hairstyle", color="ink",      style="jianghu", series="青丝"),
]

# 契约 3.3 —— 前 6 套搭配（含组成时装）
OUTFITS = [
    dict(id="of2001", n="outfit-01", title="江湖夜雨", sub="一身黑衣，半生风雪",
         items=["fi1001", "fi1010", "fi1016"], color="ink",      style="wuxia"),
    dict(id="of2002", n="outfit-02", title="庙堂朱衣", sub="满殿金辉，一人独坐",
         items=["fi1003", "fi1006", "fi1011"], color="gold",     style="court"),
    dict(id="of2003", n="outfit-03", title="竹间清谈", sub="煮茶听风，与谁共语",
         items=["fi1005", "fi1008", "fi1013"], color="bamboo",   style="scholar"),
    dict(id="of2004", n="outfit-04", title="雪落云鬓", sub="白首同归，天地无声",
         items=["fi1007", "fi1009", "fi1015"], color="moon",     style="xianxia"),
    dict(id="of2005", n="outfit-05", title="金戈铁马", sub="赤色当先，万军辟易",
         items=["fi1002", "fi1004", "fi1014"], color="cinnabar", style="wuxia"),
    dict(id="of2006", n="outfit-06", title="塞上异域", sub="风沙千里，故人不归",
         items=["fi1012", "fi1013", "fi1014"], color="purple",   style="exotic"),
]

# 契约 3.1 —— 头像
AVATARS = [
    dict(id="u1001", n="avatar-01", glyph="云", color="gold"),
    dict(id="u1002", n="avatar-02", glyph="青", color="bamboo"),
    dict(id="u1003", n="avatar-03", glyph="素", color="moon"),
]

# 各部位的版式：cx（画布宽比例）, top（高比例）, w（占宽比例）, h（占高比例）
LAYOUT = {
    "set":       (0.500, 0.238, 0.618, 0.672),
    "top":       (0.500, 0.330, 0.600, 0.452),
    "bottom":    (0.500, 0.408, 0.575, 0.508),
    "headwear":  (0.500, 0.282, 0.620, 0.352),
    "accessory": (0.500, 0.272, 0.600, 0.622),
    "hairstyle": (0.500, 0.278, 0.500, 0.580),
}

# ----------------------------------------------------------------------------
# 色彩工具
# ----------------------------------------------------------------------------
def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def lum(c):
    r, g, b = [v / 255.0 for v in c]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def _hls(c):
    return colorsys.rgb_to_hls(*[v / 255.0 for v in c])


def _from_hls(h, l, s):
    r, g, b = colorsys.hls_to_rgb(h % 1.0, max(0.0, min(1.0, l)), max(0.0, min(1.0, s)))
    return (int(r * 255), int(g * 255), int(b * 255))


def muted(c, sat=0.42, light=0.70):
    """降饱和 + 把明度抬到 light 附近 —— 古风不艳的关键。"""
    h, l, s = _hls(c)
    return _from_hls(h, l * 0.22 + light * 0.78, s * sat)


def darken_to(c, target=0.235, ink=(18, 15, 12)):
    """把颜色压到「墨色」亮度区间，作为剪影主色。"""
    l = lum(c)
    li = lum(ink)
    if l <= target:
        return mix(c, ink, 0.28)
    f = (l - target) / max(l - li, 1e-3)
    return mix(c, ink, min(0.92, f))


def palette_for(family):
    main_s, soft_s = FAMILY[family]
    main = hx(main_s)
    soft = hx(soft_s)
    deep = darken_to(main)
    return dict(
        main=main,
        soft=soft,
        # 色场：主色大幅降饱和提亮，再掺一点浅色，保证"旧纸上的淡彩"
        field=mix(muted(main, 0.40, 0.735), muted(soft, 0.34, 0.815), 0.32),
        field2=mix(muted(main, 0.52, 0.665), hx(PAPER), 0.16),
        bloom=mix(main, hx(INK_SOFT), 0.42),
        deep=deep,
        deep_line=mix(deep, (10, 9, 8), 0.48),
        inner=mix(deep, hx(PAPER), 0.40),
        glyph=mix(deep, main, 0.30),
        shadow=mix(main, hx(INK), 0.55),
    )


def seed_of(key):
    return int(hashlib.md5(key.encode("utf-8")).hexdigest()[:8], 16)


# ----------------------------------------------------------------------------
# numpy / PIL 基础工具
# ----------------------------------------------------------------------------
def _box1d(a, r, axis):
    """沿 axis 的归一化箱式模糊（cumsum 实现，O(n)）。"""
    if r < 1:
        return a
    a = np.moveaxis(a, axis, 0)
    n = a.shape[0]
    pad = np.concatenate(
        [np.repeat(a[:1], r, axis=0), a, np.repeat(a[-1:], r, axis=0)], axis=0
    ).astype(np.float32)
    c = np.zeros((pad.shape[0] + 1,) + pad.shape[1:], np.float32)
    np.cumsum(pad, axis=0, out=c[1:])
    out = (c[2 * r + 1: 2 * r + 1 + n] - c[0:n]) * (1.0 / (2 * r + 1))
    return np.moveaxis(out, 0, axis)


def blur_np(a, r, passes=3):
    """三遍箱式 ≈ 高斯。"""
    for _ in range(passes):
        a = _box1d(a, r, 0)
        a = _box1d(a, r, 1)
    return a


def resize_f(arr, size, resample=Image.BICUBIC):
    """float32 二维数组 → 目标尺寸（0..1 或任意范围都行，这里按 0..1 处理）。"""
    lo, hi = float(arr.min()), float(arr.max())
    if hi - lo < 1e-6:
        return np.full((size[1], size[0]), lo, np.float32)
    img = Image.fromarray(((arr - lo) / (hi - lo)).astype(np.float32), mode="F")
    out = np.asarray(img.resize(size, resample), dtype=np.float32)
    return out * (hi - lo) + lo


def vgrad(size, top=255, bottom=255, gamma=1.0):
    """竖直渐变 L 图。"""
    w, h = size
    ys = np.linspace(0.0, 1.0, h, dtype=np.float32) ** gamma
    col = (top + (bottom - top) * ys).astype(np.uint8)[:, None]
    return Image.fromarray(np.repeat(col, w, axis=1), mode="L")


def paint_alpha(base, color, mask, alpha=1.0):
    """用 L 蒙版把纯色刷到 base 上。"""
    if alpha <= 0.002:
        return base
    if alpha < 0.999:
        mask = mask.point(lambda v: int(v * alpha))
    solid = Image.new("RGB", base.size, color)
    base.paste(solid, (0, 0), mask)
    return base


def low_mask(alpha, full_size, resample=Image.BICUBIC):
    """0..1 float 低分辨率 alpha → 全尺寸 L 蒙版。"""
    a = np.clip(alpha, 0.0, 1.0)
    img = Image.fromarray((a * 255.0).astype(np.uint8), mode="L")
    if img.size != full_size:
        img = img.resize(full_size, resample)
    return img


def smoothstep(x, a, b):
    t = np.clip((x - a) / max(b - a, 1e-6), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def radius_mask(w, h, cx, cy, rx, ry, power=2.0, inner=0.0):
    """椭圆柔光蒙版（低分辨率）。"""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    nx = (xx / w - cx) / max(rx, 1e-4)
    ny = (yy / h - cy) / max(ry, 1e-4)
    d = np.sqrt(nx * nx + ny * ny)
    a = 1.0 - smoothstep(d, inner, 1.0)
    return np.clip(a, 0.0, 1.0) ** (1.0 / max(power, 0.2)) if power != 1.0 else np.clip(a, 0, 1)


_font_cache = {}


def font(path, size):
    key = (path, size)
    if key not in _font_cache:
        try:
            _font_cache[key] = ImageFont.truetype(path, size)
        except Exception:
            _font_cache[key] = ImageFont.truetype(FONT_SONG, size)
    return _font_cache[key]


# ----------------------------------------------------------------------------
# 宣纸底
# ----------------------------------------------------------------------------
def paper_canvas(W, H, rng):
    img = Image.new("RGB", (W, H), hx(PAPER))

    # 低频纸浆云斑（1/96 分辨率 → 放大）
    w, h = max(6, W // 96), max(6, H // 96)
    m = rng.normal(0, 1, (h, w)).astype(np.float32)
    m = blur_np(m, 2, 2)
    m = resize_f(m, (W, H))
    m = (m / (np.abs(m).max() + 1e-6) * 3.6 + 128.0)
    hi = Image.fromarray(np.clip(m, 0, 255).astype(np.uint8), "L").convert("RGB")
    img = ImageChops.add(img, hi, 1.0, -128)

    # 中频纸纤维（1/12 分辨率）
    w, h = max(8, W // 12), max(8, H // 12)
    f = rng.normal(0, 1, (h, w)).astype(np.float32)
    f = blur_np(f, 1, 2)
    f = resize_f(f, (W, H))
    f = f / (np.abs(f).max() + 1e-6) * 3.0 + 128.0
    img = ImageChops.add(img, Image.fromarray(np.clip(f, 0, 255).astype(np.uint8), "L").convert("RGB"), 1.0, -128)

    # 全分辨率细颗粒
    g = rng.normal(0, 1.25, (H, W)).astype(np.float32) + 128.0
    img = ImageChops.add(img, Image.fromarray(np.clip(g, 0, 255).astype(np.uint8), "L").convert("RGB"), 1.0, -128)

    # 极轻柔化，去掉数码噪点的锐利感
    return img.filter(ImageFilter.GaussianBlur(0.4))


# ----------------------------------------------------------------------------
# 贝塞尔路径 & 六类服装轮廓（归一化坐标：x∈[-1,1]，y∈[0,1]）
# ----------------------------------------------------------------------------
class P:
    def __init__(self, x, y):
        self.pts = [(float(x), float(y))]

    def L(self, x, y):
        self.pts.append((float(x), float(y)))
        return self

    def C(self, c1x, c1y, c2x, c2y, x, y, n=24):
        x0, y0 = self.pts[-1]
        t = np.linspace(0.0, 1.0, n, dtype=np.float32)[1:]
        mt = 1.0 - t
        bx = (mt ** 3) * x0 + 3 * (mt ** 2) * t * c1x + 3 * mt * (t ** 2) * c2x + (t ** 3) * x
        by = (mt ** 3) * y0 + 3 * (mt ** 2) * t * c1y + 3 * mt * (t ** 2) * c2y + (t ** 3) * y
        self.pts.extend(zip(bx.tolist(), by.tolist()))
        return self


def mirror(pts):
    """半侧路径（上中 → 下中）补成闭合全形。"""
    left = [(-x, y) for (x, y) in reversed(pts[1:-1])]
    return pts + left


def ellipse_pts(cx, cy, rx, ry, n=56, rot=0.0):
    t = np.linspace(0, 2 * math.pi, n, endpoint=False)
    xs = cx + np.cos(t) * rx
    ys = cy + np.sin(t) * ry
    if rot:
        ca, sa = math.cos(rot), math.sin(rot)
        xs, ys = cx + (xs - cx) * ca - (ys - cy) * sa, cy + (xs - cx) * sa + (ys - cy) * ca
    return list(zip(xs.tolist(), ys.tolist()))


def shape_set(v):
    """整套袍：立领交领、广袖垂坠、收腰、下摆微张（平铺示意，前视）。"""
    sl = v["sleeve"]
    fl = v["flare"]
    p = P(0.0, 0.052)
    p.C(0.062, 0.040, 0.180, 0.050, 0.284, 0.088)         # 颈 → 肩头
    p.C(0.406, 0.146, 0.528, 0.232, 0.596, 0.330)         # 袖外沿下垂
    p.C(0.646 * sl, 0.404, 0.678 * sl, 0.480, 0.684 * sl, 0.548)   # 袖外角
    p.C(0.672 * sl, 0.590, 0.598, 0.612, 0.540, 0.600)    # 宽袖口
    p.C(0.486, 0.590, 0.428, 0.566, 0.398, 0.520)         # 袖内沿 → 肋
    p.C(0.352, 0.588, 0.330, 0.648, 0.352, 0.702)         # 收腰
    p.C(0.396, 0.784, 0.468, 0.870, 0.516 * fl, 0.958)    # 下摆外张
    p.C(0.506 * fl, 1.008, 0.300, 1.010, 0.0, 1.006)      # 摆弧
    fill = [mirror(p.pts)]

    # 交领内襟（浅色布料，低透明）
    q = P(0.0, 0.050)
    q.C(0.100, 0.040, 0.186, 0.056, 0.248, 0.084)
    q.C(0.198, 0.188, 0.132, 0.302, 0.030, 0.376)
    q.L(0.0, 0.390)
    q.L(-0.030, 0.376)
    q.C(-0.132, 0.302, -0.198, 0.188, -0.248, 0.084)
    q.C(-0.186, 0.056, -0.100, 0.040, 0.0, 0.050)
    inner = [q.pts]

    # 腰带（横束）——用浅色填一块，结构立刻清楚
    s0, s1 = 0.640, 0.716
    sash = P(-0.346, s0)
    sash.C(-0.190, s0 - 0.012, 0.190, s0 + 0.012, 0.346, s0)
    sash.L(0.344, s1)
    sash.C(0.190, s1 + 0.012, -0.190, s1 - 0.012, -0.344, s1)
    inner.append(sash.pts)

    det = []
    # 门襟（交领 V）
    det.append([(0.0, 0.050), (0.104, 0.180), (0.150, 0.318), (0.074, 0.372)])
    det.append([(0.0, 0.050), (-0.062, 0.140), (-0.098, 0.300)])
    # 腰带上下沿
    det.append([(-0.344, s0), (0.0, s0 + 0.010), (0.344, s0)])
    det.append([(-0.344, s1), (0.0, s1 + 0.010), (0.344, s1)])
    # 袖口弧
    det.append([(0.596, 0.556), (0.646, 0.576), (0.678, 0.548)])
    det.append([(-0.596, 0.556), (-0.646, 0.576), (-0.678, 0.548)])
    # 袖褶
    det.append([(0.470, 0.300), (0.540, 0.420), (0.596, 0.540)])
    det.append([(-0.470, 0.300), (-0.540, 0.420), (-0.596, 0.540)])
    # 裙褶（自腰辐射到摆）
    for xx in (0.075, 0.165, 0.250):
        det.append([(xx, s1 + 0.010), (xx * 1.35, 0.860), (xx * 1.62, 0.996)])
        det.append([(-xx, s1 + 0.010), (-xx * 1.35, 0.860), (-xx * 1.62, 0.996)])
    det.append([(0.0, s1 + 0.010), (0.0, 1.002)])
    return dict(fill=fill, inner=inner, detail=det, stroke=fill)


def shape_top(v):
    """上衣：广袖长垂，衣身收在腰胯。"""
    sl = v["sleeve"]
    fl = v["flare"]
    p = P(0.0, 0.060)
    p.C(0.072, 0.046, 0.188, 0.058, 0.290, 0.096)
    p.C(0.424, 0.172, 0.556, 0.272, 0.634, 0.396)
    p.C(0.678 * sl, 0.476, 0.706 * sl, 0.562, 0.706 * sl, 0.646)
    p.C(0.700, 0.706, 0.646, 0.730, 0.588, 0.710)
    p.C(0.518, 0.680, 0.442, 0.644, 0.368, 0.632)
    p.C(0.330, 0.746, 0.322 * fl, 0.874, 0.326 * fl, 0.996)
    p.C(0.216, 1.006, 0.108, 0.998, 0.0, 1.000)
    fill = [mirror(p.pts)]

    q = P(0.0, 0.058)
    q.C(0.100, 0.048, 0.186, 0.064, 0.246, 0.092)
    q.C(0.194, 0.196, 0.128, 0.306, 0.028, 0.376)
    q.L(0.0, 0.390)
    q.L(-0.028, 0.376)
    q.C(-0.128, 0.306, -0.194, 0.196, -0.246, 0.092)
    q.C(-0.186, 0.064, -0.100, 0.048, 0.0, 0.058)
    inner = [q.pts]

    det = []
    det.append([(0.0, 0.058), (0.102, 0.186), (0.146, 0.320), (0.070, 0.376)])
    det.append([(0.0, 0.058), (-0.064, 0.146), (-0.100, 0.306)])
    det.append([(0.606, 0.676), (0.654, 0.696), (0.692, 0.664)])
    det.append([(-0.606, 0.676), (-0.654, 0.696), (-0.692, 0.664)])
    det.append([(0.470, 0.360), (0.546, 0.500), (0.600, 0.650)])
    det.append([(-0.470, 0.360), (-0.546, 0.500), (-0.600, 0.650)])
    det.append([(-0.324, 0.968), (0.0, 0.980), (0.324, 0.968)])
    det.append([(0.0, 0.392), (0.0, 0.986)])
    for xx in (0.078, 0.165):
        det.append([(xx, 0.800), (xx * 1.30, 0.900), (xx * 1.42, 0.988)])
        det.append([(-xx, 0.800), (-xx * 1.30, 0.900), (-xx * 1.42, 0.988)])
    return dict(fill=fill, inner=inner, detail=det, stroke=fill)


def shape_bottom(v):
    """下装：束腰罗裙，腰头 + 胯部收 + A 字散摆 + 褶裥。"""
    fl = v["flare"]
    p = P(0.0, 0.050)
    p.C(0.098, 0.034, 0.238, 0.032, 0.320, 0.058)
    p.C(0.374, 0.142, 0.430, 0.248, 0.488, 0.362)
    p.C(0.566 * fl, 0.516, 0.652 * fl, 0.700, 0.716 * fl, 0.856)
    p.C(0.746 * fl, 0.922, 0.762 * fl, 0.966, 0.768 * fl, 0.998)
    p.C(0.560, 1.026, 0.288, 1.008, 0.0, 1.014)
    fill = [mirror(p.pts)]

    # 腰头
    wb = P(-0.324, 0.052)
    wb.C(-0.210, 0.026, -0.100, 0.024, 0.0, 0.028)
    wb.C(0.100, 0.024, 0.210, 0.026, 0.324, 0.052)
    wb.L(0.318, 0.140)
    wb.C(0.208, 0.114, 0.098, 0.112, 0.0, 0.116)
    wb.C(-0.098, 0.112, -0.208, 0.114, -0.318, 0.140)
    inner = [wb.pts]

    det = []
    det.append([(-0.318, 0.142), (0.0, 0.120), (0.318, 0.142)])
    for xx in (0.060, 0.150, 0.240, 0.330, 0.420):
        det.append([(xx * 0.34, 0.148), (xx * 0.80, 0.560), (xx * 1.02, 0.996)])
        det.append([(-xx * 0.34, 0.148), (-xx * 0.80, 0.560), (-xx * 1.02, 0.996)])
    det.append([(-0.560, 0.956), (-0.290, 1.000), (0.0, 1.010), (0.290, 1.000), (0.560, 0.956)])
    return dict(fill=fill, inner=inner, detail=det, stroke=fill)


def shape_headwear(v):
    """头饰：斗笠 —— 圆锥帽身 + 微翘宽檐 + 顶珠 + 系带。"""
    wd = v["wide"]
    p = P(0.0, 0.050)
    p.C(0.104, 0.092, 0.340, 0.252, 0.578, 0.416)
    p.C(0.724, 0.516, 0.862 * wd, 0.572, 0.962 * wd, 0.590)
    p.C(0.660, 0.664, 0.360, 0.716, 0.0, 0.726)          # 檐下缘（中间最低）
    fill = [mirror(p.pts)]
    # 顶珠
    fill.append(ellipse_pts(0.0, 0.030, 0.046, 0.030, 36))

    # 檐口内面（浅色）
    inner = []
    x = np.linspace(-0.962, 0.962, 61)
    up = 0.590 + 0.136 * (1.0 - (np.abs(x) / 0.962) ** 1.35)
    lo = 0.606 + 0.140 * (1.0 - (np.abs(x) / 0.962) ** 1.35)
    band = list(zip(x.tolist(), (up - 0.012).tolist())) + list(zip(x[::-1].tolist(), (lo + 0.006)[::-1].tolist()))
    inner.append(band)

    det = []
    # 檐口线
    det.append(list(zip(x.tolist(), (up + 0.004).tolist())))
    # 帽身竹骨（两段弧）
    for k in (0.52, 0.78):
        xx = np.linspace(-0.58 * k, 0.58 * k, 21)
        yy = 0.098 + (0.416 - 0.098) * (np.abs(xx) / 0.578) ** 0.86 * 1.02
        det.append(list(zip(xx.tolist(), yy.tolist())))
    # 系带
    for s in (1, -1):
        det.append([(s * 0.052, 0.716), (s * 0.112, 0.826), (s * 0.150, 0.958)])
    return dict(fill=fill, inner=inner, detail=det, stroke=fill[:1])


def shape_accessory(v):
    """配饰：披风（两片开襟）+ 如意云肩。"""
    wd = v["wide"]

    def panel(sign):
        p = P(sign * 0.148, 0.062)
        p.C(sign * 0.244, 0.046, sign * 0.336, 0.058, sign * 0.404, 0.096)
        p.C(sign * 0.550, 0.204, sign * 0.666 * wd, 0.348, sign * 0.712 * wd, 0.498)
        p.C(sign * 0.742 * wd, 0.606, sign * 0.752 * wd, 0.740, sign * 0.728 * wd, 0.856)
        p.C(sign * 0.700 * wd, 0.936, sign * 0.552, 0.966, sign * 0.404, 0.940)
        p.C(sign * 0.296, 0.858, sign * 0.228, 0.700, sign * 0.206, 0.512)
        p.C(sign * 0.194, 0.352, sign * 0.184, 0.180, sign * 0.148, 0.062)
        return p.pts

    fill = [panel(1), panel(-1)]

    # 如意云肩：上缘贴合肩颈，下缘五瓣
    xs = np.linspace(-0.452, 0.452, 151)
    u = (xs + 0.452) / 0.904
    top = 0.068 + 0.056 * (np.abs(xs) / 0.452) ** 1.25
    lobe = np.abs(np.sin(u * 5.0 * np.pi)) ** 0.85
    bot = 0.150 + 0.086 * lobe + 0.036 * (1.0 - np.abs(2 * u - 1))
    cloud = list(zip(xs.tolist(), top.tolist())) + list(zip(xs[::-1].tolist(), bot[::-1].tolist()))
    inner = [cloud]

    det = []
    det.append(list(zip(xs.tolist(), top.tolist())))
    det.append(list(zip(xs.tolist(), bot.tolist())))
    for i in range(1, 5):
        x = -0.452 + 0.904 * i / 5.0
        det.append([(x * 0.90, 0.108), (x * 1.02, 0.186), (x * 0.94, 0.234)])
    # 披风门襟 + 垂坠褶皱
    for sign in (1, -1):
        det.append([(sign * 0.148, 0.062), (sign * 0.212, 0.260), (sign * 0.240, 0.560),
                    (sign * 0.286, 0.780), (sign * 0.362, 0.930)])
        det.append([(sign * 0.330, 0.120), (sign * 0.436, 0.360), (sign * 0.512, 0.640), (sign * 0.560, 0.916)])
        det.append([(sign * 0.520, 0.190), (sign * 0.612, 0.430), (sign * 0.674, 0.700), (sign * 0.680, 0.900)])
        det.append([(sign * 0.250, 0.902), (sign * 0.430, 0.938), (sign * 0.590, 0.918)])
    return dict(fill=fill, inner=inner, detail=det, stroke=fill)


def shape_hairstyle(v):
    """发型：垂鬟 —— 发团 + 顶髻 + 双垂发 + 发簪。"""
    # 发团（头形，略扁）
    head = []
    hp = P(-0.372, 0.396)
    hp.C(-0.372, 0.226, -0.208, 0.128, 0.0, 0.124)
    hp.C(0.208, 0.128, 0.372, 0.226, 0.372, 0.396)
    hp.C(0.372, 0.520, 0.236, 0.586, 0.0, 0.592)
    hp.C(-0.236, 0.586, -0.372, 0.520, -0.372, 0.396)
    head.append(hp.pts)
    # 顶髻 + 髻座
    bun = ellipse_pts(0.0, 0.142, 0.222, 0.108, 60)
    knot = ellipse_pts(0.0, 0.052, 0.086, 0.056, 40)
    # 垂发（贴颊而下，末端收尖）
    def lock(sign):
        p = P(sign * 0.318, 0.212)
        p.C(sign * 0.402, 0.330, sign * 0.418, 0.470, sign * 0.404, 0.610)
        p.C(sign * 0.396, 0.740, sign * 0.372, 0.870, sign * 0.330, 0.972)
        p.C(sign * 0.268, 0.930, sign * 0.232, 0.840, sign * 0.226, 0.730)
        p.C(sign * 0.236, 0.560, sign * 0.252, 0.390, sign * 0.318, 0.212)
        return p.pts

    fill = [head[0], bun, knot, lock(1), lock(-1)]

    # 发簪（横斜过髻）+ 簪头
    det = []
    det.append([(-0.348, 0.196), (-0.110, 0.128), (0.140, 0.086), (0.318, 0.048)])
    # 发丝
    for s in (1, -1):
        det.append([(s * 0.060, 0.132), (s * 0.230, 0.170), (s * 0.330, 0.300), (s * 0.352, 0.460)])
        det.append([(s * 0.150, 0.196), (s * 0.286, 0.360), (s * 0.344, 0.560), (s * 0.300, 0.760)])
        det.append([(s * 0.196, 0.640), (s * 0.312, 0.760), (s * 0.300, 0.930)])
    det.append([(-0.340, 0.398), (-0.170, 0.340), (0.0, 0.322), (0.170, 0.340), (0.340, 0.398)])
    # 顶髻盘发纹
    det.append([(-0.180, 0.108), (-0.060, 0.076), (0.080, 0.086), (0.180, 0.130)])
    return dict(fill=fill, inner=[], detail=det, stroke=[head[0], bun])


SHAPES = {
    "set": shape_set,
    "top": shape_top,
    "bottom": shape_bottom,
    "headwear": shape_headwear,
    "accessory": shape_accessory,
    "hairstyle": shape_hairstyle,
}


def variant_for(rng, cat):
    """每件时装的轮廓微差（同类别不同剪影宽度/袖长/摆幅）。"""
    return dict(
        sleeve=rng.uniform(0.94, 1.06),
        flare=rng.uniform(0.93, 1.07),
        wide=rng.uniform(0.94, 1.06),
    )


# ----------------------------------------------------------------------------
# 剪影绘制
# ----------------------------------------------------------------------------
def box_map(box, W, H, ss_pts):
    cx, top, wid, hei = box
    hw = wid * W * 0.5
    hh = hei * H
    ox, oy = cx * W, top * H
    return lambda pts: [(ox + x * hw, oy + y * hh) for (x, y) in pts]


def form_gradient(W, H, c_top, c_mid, c_bot, shift=0.16):
    """低分辨率 RGB 形体渐变：上亮下暗、并带一点侧向偏斜，让剪影不像平涂贴纸。"""
    lw, lh = max(16, W // 64), max(16, H // 64)
    yy, xx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    ny = yy / max(lh - 1, 1)
    nx = xx / max(lw - 1, 1)
    t = np.clip(ny * 0.86 + (nx - 0.5) * shift + 0.06, 0.0, 1.0)
    t = t * t * (3.0 - 2.0 * t)
    a = np.array(c_top, np.float32)
    b = np.array(c_mid, np.float32)
    c = np.array(c_bot, np.float32)
    lo = a[None, None, :] + (b - a)[None, None, :] * (t[:, :, None] / 0.55)
    hi = b[None, None, :] + (c - b)[None, None, :] * np.clip((t[:, :, None] - 0.55) / 0.45, 0, 1)
    rgb = np.where((t[:, :, None] < 0.55), lo, hi)
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB")
    return img.resize((W, H), Image.BICUBIC)


def draw_shape(canvas, cat, box, pal, rng, ss, alpha=0.55, with_stroke=True, blur_shadow=True):
    W, H = canvas.size
    shape = SHAPES[cat](variant_for(rng, cat))
    mp = box_map(box, W, H, ss)

    # ---- 墨影：让剪影"长"在纸上（低分辨率大模糊）
    if blur_shadow:
        sw, sh = W // 10, H // 10
        m = Image.new("L", (sw, sh), 0)
        d = ImageDraw.Draw(m)
        for poly in shape["fill"]:
            d.polygon([(x / 10, y / 10) for (x, y) in mp(poly)], fill=255)
        arr = blur_np(np.asarray(m, np.float32) / 255.0, 5, 3)
        sh_mask = low_mask(arr * 0.85, (W, H))
        paint_alpha(canvas, pal["shadow"], sh_mask, 0.12)

    # ---- 填充：用形体渐变而非纯色，布料才有转折
    mask = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(mask)
    for poly in shape["fill"]:
        d.polygon(mp(poly), fill=255)
    mask = ImageChops.multiply(mask, vgrad((W, H), 196, 252))
    if alpha < 0.999:
        mask = mask.point(lambda v: int(v * alpha))
    grad = form_gradient(W, H, mix(pal["deep"], pal["inner"], 0.34), pal["deep"], pal["deep_line"])
    canvas.paste(grad, (0, 0), mask)

    # ---- 内层布料（交领 / 腰带 / 云肩 / 腰头）
    if shape["inner"]:
        m2 = Image.new("L", (W, H), 0)
        d2 = ImageDraw.Draw(m2)
        for poly in shape["inner"]:
            d2.polygon(mp(poly), fill=255)
        m2 = ImageChops.multiply(m2, vgrad((W, H), 214, 255))
        paint_alpha(canvas, pal["inner"], m2, 0.34)

    # ---- 褶皱 / 门襟 / 绦带细线
    if shape["detail"]:
        md = Image.new("L", (W, H), 0)
        dd = ImageDraw.Draw(md)
        lw = max(1, int(round(1.1 * ss)))
        for line in shape["detail"]:
            pp = mp(line)
            if len(pp) >= 2:
                dd.line(pp, fill=255, width=lw, joint="curve")
        paint_alpha(canvas, pal["deep_line"], md, 0.20)

    # ---- 1.5px 绣线描边
    if with_stroke and shape["stroke"]:
        ms = Image.new("L", (W, H), 0)
        ds = ImageDraw.Draw(ms)
        wst = max(1, int(round(1.5 * ss)))
        for poly in shape["stroke"]:
            pp = mp(poly)
            ds.line(pp + [pp[0]], fill=255, width=wst, joint="curve")
        paint_alpha(canvas, pal["deep_line"], ms, 0.66)


# ----------------------------------------------------------------------------
# 色场 / 墨晕 / 装饰
# ----------------------------------------------------------------------------
def draw_field(canvas, pal, rng, W, H):
    """径向柔光 + 斜向光带，两个色场叠加成"淡彩"。"""
    lw, lh = max(32, W // 8), max(32, H // 8)
    yy, xx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    nx, ny = xx / lw, yy / lh

    # 斜向光带
    th = math.radians(72 + rng.uniform(-12, 12))
    proj = nx * math.cos(th) + ny * math.sin(th)
    band = 1.0 - np.abs(proj - (0.44 + rng.uniform(-0.05, 0.05))) / 0.40
    band = np.clip(band, 0.0, 1.0) ** 1.35
    band *= 0.55

    # 径向柔光（位置随种子浮动，避免 16 张一模一样）
    rad = radius_mask(lw, lh, 0.50 + rng.uniform(-0.05, 0.05), 0.40 + rng.uniform(-0.05, 0.05),
                      0.50, 0.42, power=1.15)

    a1 = np.clip(rad * 0.62 + band * 0.30, 0.0, 1.0) * 0.62
    # 纸纹吸收：让色场边缘毛一点，不死板
    grain = rng.normal(0, 0.03, (lh, lw)).astype(np.float32)
    a1 = np.clip(a1 + grain, 0.0, 1.0)
    paint_alpha(canvas, pal["field"], low_mask(a1, (W, H)), 0.90)

    # 第二层：更靠下、更淡，制造上下色的呼吸
    rad2 = radius_mask(lw, lh, 0.46 + rng.uniform(-0.06, 0.06), 0.72 + rng.uniform(-0.05, 0.05),
                       0.58, 0.34, power=1.25)
    a2 = np.clip(rad2 * 0.52, 0.0, 1.0) * 0.55
    paint_alpha(canvas, pal["field2"], low_mask(a2, (W, H)), 0.85)


def draw_blooms(canvas, pal, rng, W, H, count=3):
    """墨晕：多边形随机扰动 + 圆滑 + 三遍模糊，1/4 分辨率累加后放大。"""
    lw, lh = max(40, W // 4), max(40, H // 4)
    acc = np.zeros((lh, lw), np.float32)
    for i in range(count):
        cx = rng.uniform(0.30, 0.72)
        cy = rng.uniform(0.32, 0.80)
        rx = rng.uniform(0.14, 0.26) * lw
        ry = rng.uniform(0.10, 0.19) * lh
        m = Image.new("L", (lw, lh), 0)
        d = ImageDraw.Draw(m)
        n = 26
        ang = np.linspace(0, 2 * math.pi, n, endpoint=False)
        rad = rng.uniform(0.56, 1.0, n)
        for _ in range(4):
            rad = (np.roll(rad, 1) + rad + np.roll(rad, -1)) / 3.0
        pts = []
        for a, r in zip(ang, rad):
            k = 1.0 + 0.35 * (r - 0.78)
            pts.append((cx * lw + math.cos(a) * rx * k, cy * lh + math.sin(a) * ry * k))
        d.polygon(pts, fill=255)
        arr = np.asarray(m, np.float32) / 255.0
        arr = blur_np(arr, int(max(4, min(lw, lh) * 0.05)), 3)
        acc += arr * rng.uniform(0.16, 0.30)
    acc = np.clip(acc, 0.0, 0.5)
    paint_alpha(canvas, pal["bloom"], low_mask(acc, (W, H)), 0.85)


def draw_vignette(canvas, W, H):
    lw, lh = max(32, W // 16), max(32, H // 16)
    yy, xx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    nx, ny = xx / lw - 0.5, yy / lh - 0.5
    d = np.sqrt(nx * nx * 1.05 + ny * ny * 0.85)
    a = smoothstep(d, 0.36, 0.78) * 0.34
    paint_alpha(canvas, hx(INK), low_mask(np.clip(a, 0, 1), (W, H)), 0.75)


def draw_bottom_shade(canvas, W, H):
    """底部 22% → rgba(16,14,12,0.55)，App 卡片叠白字可读。"""
    lw, lh = 8, max(16, H // 24)
    yy = np.linspace(0, 1, lh, dtype=np.float32)[:, None]
    a = smoothstep(yy, 0.02, 1.0) * 0.55
    a = np.repeat(a, lw, axis=1)
    m = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L")
    m = m.resize((W, int(H * 0.22)), Image.BICUBIC)
    full = Image.new("L", (W, H), 0)
    full.paste(m, (0, H - int(H * 0.22)))
    paint_alpha(canvas, hx(INK), full, 1.0)


def draw_frame(canvas, ss, inset=40, color=GOLD, gap=12):
    """金线内框：四角留 12px 缺口，并用 45° 斜线倒角接合 —— 传统笺纸的做法。"""
    W, H = canvas.size
    inset *= ss
    gap *= ss
    lw = max(1, int(round(1.5 * ss)))
    m = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(m)
    x0, y0, x1, y1 = inset, inset, W - inset, H - inset
    g = gap
    # 四边（角上断开 12px）
    d.line([(x0 + g, y0), (x1 - g, y0)], fill=255, width=lw)
    d.line([(x0 + g, y1), (x1 - g, y1)], fill=255, width=lw)
    d.line([(x0, y0 + g), (x0, y1 - g)], fill=255, width=lw)
    d.line([(x1, y0 + g), (x1, y1 - g)], fill=255, width=lw)
    # 四角斜接
    for (cx, cy, sx, sy) in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        d.line([(cx + sx * g, cy), (cx, cy + sy * g)], fill=255, width=lw)
    paint_alpha(canvas, hx(color), m, 0.80)

    # 四角内的短饰线（12px，与框线垂直，纯装饰）
    m2 = Image.new("L", (W, H), 0)
    d2 = ImageDraw.Draw(m2)
    o = int(round(9 * ss))
    tk = int(round(12 * ss))
    for (cx, cy, sx, sy) in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        d2.line([(cx + sx * (g + o), cy + sy * o), (cx + sx * (g + o + tk), cy + sy * o)],
                fill=255, width=lw)
        d2.line([(cx + sx * o, cy + sy * (g + o)), (cx + sx * o, cy + sy * (g + o + tk))],
                fill=255, width=lw)
    paint_alpha(canvas, hx(color), m2, 0.55)


def draw_vtext(canvas, chars, cx, y0, size, color, alpha=0.75, spacing=1.20,
               font_path=FONT_KAI, char_gap=0.0):
    """竖排文字（自绘蒙版，可控透明度）。"""
    m = Image.new("L", canvas.size, 0)
    d = ImageDraw.Draw(m)
    f = font(font_path, int(size))
    y = y0 + size * 0.5
    for ch in chars:
        d.text((cx, y), ch, font=f, fill=255, anchor="mm")
        y += size * spacing + char_gap
    paint_alpha(canvas, color, m, alpha)


def draw_seal(canvas, ss, cx, cy, size, char="燕"):
    """极淡的方形印章，只做点睛（透明度很低，不与主字抢戏）。"""
    W, H = canvas.size
    s = size * ss
    m = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(m)
    r = [cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2]
    d.rectangle(r, outline=255, width=max(1, int(round(1.2 * ss))))
    d.text((cx, cy), char, font=font(FONT_KAI, int(s * 0.62)), fill=200, anchor="mm")
    paint_alpha(canvas, hx(SEAL), m, 0.30)


# ----------------------------------------------------------------------------
# 时装海报
# ----------------------------------------------------------------------------
def render_fashion(item, ss=4, preview=False):
    rng = np.random.default_rng(seed_of(item["id"]))
    W, H = POSTER[0] * ss, POSTER[1] * ss
    pal = palette_for(item["color"])

    canvas = paper_canvas(W, H, rng)
    draw_field(canvas, pal, rng, W, H)
    draw_blooms(canvas, pal, rng, W, H, count=int(rng.integers(2, 5)))
    draw_vignette(canvas, W, H)

    cx, top, wid, hei = LAYOUT[item["cat"]]
    draw_shape(canvas, item["cat"], (cx, top, wid, hei), pal, rng, ss)

    draw_frame(canvas, ss)

    # 右上角竖排单字 + 极细竖线 + 竖排系列名（一个完整的小版式块）
    gx = W * 0.858
    gsize = 118 * ss
    gy = H * 0.108
    gtop = gy - gsize * 0.06
    gbot = gy + gsize * 1.06
    rule = Image.new("L", (W, H), 0)
    ImageDraw.Draw(rule).line([(W * 0.905, gtop), (W * 0.905, gbot)],
                              fill=255, width=max(1, int(round(1.0 * ss))))
    paint_alpha(canvas, hx(GOLD), rule, 0.42)
    draw_vtext(canvas, item["glyph"], gx, gy, gsize, pal["glyph"], 0.80, spacing=1.0)
    ssize = 28 * ss
    draw_vtext(canvas, item["series"], gx, gbot + 16 * ss, ssize, pal["glyph"], 0.52, spacing=1.24)

    draw_bottom_shade(canvas, W, H)

    out = canvas.resize(POSTER, Image.LANCZOS)
    return out


# ----------------------------------------------------------------------------
# 搭配封面
# ----------------------------------------------------------------------------
def render_outfit(outfit, ss=4):
    rng = np.random.default_rng(seed_of(outfit["id"]))
    W, H = POSTER[0] * ss, POSTER[1] * ss
    pal = palette_for(outfit["color"])

    canvas = paper_canvas(W, H, rng)
    draw_field(canvas, pal, rng, W, H)
    draw_blooms(canvas, pal, rng, W, H, count=int(rng.integers(2, 4)))
    draw_vignette(canvas, W, H)

    # 组成时装：主件大、配件小，错落叠印
    by_id = {it["id"]: it for it in ITEMS}
    comp = [by_id[i] for i in outfit["items"]]
    comp.sort(key=lambda it: 0 if it["cat"] in ("set", "top", "accessory") else 1)

    placed = [
        dict(box=(0.615, 0.300, 0.560, 0.600), alpha=0.50, stroke=True, blur=True),
        dict(box=(0.820, 0.248, 0.330, 0.330), alpha=0.34, stroke=False, blur=True),
        dict(box=(0.400, 0.520, 0.380, 0.420), alpha=0.24, stroke=False, blur=True),
    ]
    for i, it in enumerate(comp[:3]):
        pl = placed[i]
        draw_shape(canvas, it["cat"], pl["box"], pal, rng, ss,
                   alpha=pl["alpha"], with_stroke=pl["stroke"], blur_shadow=pl["blur"])

    draw_frame(canvas, ss)

    # 竖排标题（左）+ 副标题（更小，右侧）
    tsize = 104 * ss
    tx = W * 0.152
    draw_vtext(canvas, outfit["title"], tx, H * 0.128, tsize, pal["glyph"], 0.80, spacing=1.16)
    draw_vtext(canvas, outfit["sub"], W * 0.238, H * 0.150, 26 * ss, pal["glyph"], 0.52, spacing=1.34)

    draw_bottom_shade(canvas, W, H)
    out = canvas.resize(POSTER, Image.LANCZOS)
    return out


# ----------------------------------------------------------------------------
# 首页主视觉
# ----------------------------------------------------------------------------
def ridge_1d(n, rng, octaves=7, decay=0.55, smooth=3):
    xs = [0.0, 1.0]
    ys = list(rng.normal(0, 1, 2))
    d = 1.0
    for _ in range(octaves):
        nx, ny = [], []
        for i in range(len(xs) - 1):
            nx.append(xs[i])
            ny.append(ys[i])
            nx.append(0.5 * (xs[i] + xs[i + 1]))
            ny.append(0.5 * (ys[i] + ys[i + 1]) + rng.normal(0, 1) * d)
        nx.append(xs[-1])
        ny.append(ys[-1])
        xs, ys = nx, ny
        d *= decay
    xs = np.array(xs)
    ys = np.array(ys)
    s = np.linspace(0, 1, n)
    yy = np.interp(s, xs, ys)
    for _ in range(smooth):
        yy = (np.roll(yy, 1) + yy + np.roll(yy, -1)) / 3.0
    yy = yy - np.linspace(yy[0], yy[-1], n)
    return yy / (np.abs(yy).max() + 1e-6)


def draw_mountain(canvas, rng, W, H, level, ss, layers=4):
    """水墨远山：越远越淡越蓝灰，左侧渐隐留给标题。"""
    t = level / max(layers - 1, 1)
    base_y = 0.470 + 0.150 * level
    ampx = 0.098 - 0.012 * level
    shade = 0.86 - 0.20 * level           # 与纸的混合比例：越大越淡
    alpha = 0.34 + 0.10 * level
    col = mix(hx("#38485A"), hx(PAPER), max(0.10, min(0.90, shade)))
    if level == layers - 1:
        col = mix(hx("#2B3947"), hx(PAPER), 0.26)

    n = 900
    yy = ridge_1d(n, rng, octaves=7, decay=0.56)
    s = np.linspace(0, 1, n)
    # 右高左低 + 边缘锚定
    yfrac = base_y + yy * ampx - 0.115 * s
    yfrac = np.clip(yfrac, 0.16, 0.995)

    if level <= 1:
        # 远山在 1/2 分辨率绘制后放大 → 天然柔边（大气透视）
        w2, h2 = W // 2, H // 2
        m2 = Image.new("L", (w2, h2), 0)
        d = ImageDraw.Draw(m2)
        pts = [(i * (w2 - 1) / (n - 1), float(yfrac[i]) * h2) for i in range(n)]
        d.polygon(pts + [(w2, h2), (0, h2)], fill=255)
        m = m2.resize((W, H), Image.BICUBIC)
    else:
        m = Image.new("L", (W, H), 0)
        d = ImageDraw.Draw(m)
        pts = [(i * (W - 1) / (n - 1), float(yfrac[i]) * H) for i in range(n)]
        d.polygon(pts + [(W, H), (0, H)], fill=255)

    # 左侧 40% 渐隐
    lw = W // 16
    xs = np.linspace(0, 1, lw, dtype=np.float32)
    ramp = smoothstep(xs, 0.14, 0.52)
    ramp_img = Image.fromarray(np.repeat((ramp * 255).astype(np.uint8)[None, :], H, axis=0), "L")
    if ramp_img.size != (W, H):
        ramp_img = ramp_img.resize((W, H), Image.BILINEAR)
    m = ImageChops.multiply(m, ramp_img)

    paint_alpha(canvas, col, m, alpha)
    return yfrac


def draw_mist(canvas, rng, W, H, y, thick, alpha, ss):
    lw, lh = max(16, W // 8), max(8, H // 8)
    yy, xx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    nx, ny = xx / lw, yy / lh
    band = np.exp(-((ny - y) ** 2) / (2 * (thick ** 2)))
    band *= (0.75 + 0.25 * np.sin(nx * 9.0 + rng.uniform(0, 6)))
    band = blur_np(band.astype(np.float32), 3, 3)
    band = np.clip(band * alpha, 0, 1)
    band *= smoothstep(nx, 0.02, 0.30)
    paint_alpha(canvas, hx(PAPER_HI), low_mask(band, (W, H)), 0.9)


def draw_figure(canvas, cx, feet_y, height, color, alpha, W, H, ss):
    """右侧人物剪影：斗笠 + 长袍 + 广袖迎风 + 小刀，简笔但可辨识。"""
    hw = height * 0.30
    hh = height
    ox = cx * W
    oy = feet_y * H - hh

    def mp(pts):
        return [(ox + x * hw, oy + y * hh) for (x, y) in pts]

    # 斗笠
    hat = []
    hp = P(-1.05, 0.150)
    hp.C(-0.72, 0.120, -0.34, 0.030, 0.0, 0.006)
    hp.C(0.34, 0.030, 0.72, 0.120, 1.05, 0.150)
    hp.C(0.66, 0.196, 0.28, 0.206, 0.0, 0.200)
    hp.C(-0.28, 0.206, -0.66, 0.196, -1.05, 0.150)
    hat.append(hp.pts)
    hat.append(ellipse_pts(0.0, 0.022, 0.10, 0.028, 24))

    # 头 / 颈
    head = ellipse_pts(0.02, 0.245, 0.115, 0.120, 40)
    neck = [(-0.055, 0.330), (0.075, 0.330), (0.090, 0.410), (-0.070, 0.410)]

    # 袍身（迎风向左侧鼓）
    body = []
    p = P(-0.180, 0.360)
    p.C(-0.300, 0.470, -0.320, 0.620, -0.300, 0.760)
    p.C(-0.560, 0.780, -0.900, 0.830, -1.020, 0.960)
    p.C(-0.620, 1.010, -0.220, 1.020, 0.030, 1.000)
    p.C(0.260, 0.980, 0.430, 0.960, 0.480, 0.900)
    p.C(0.430, 0.700, 0.360, 0.480, 0.240, 0.372)
    p.C(0.120, 0.336, -0.060, 0.342, -0.180, 0.360)
    body.append(p.pts)

    # 左袖（迎风大袖）
    sleeve = []
    sp = P(-0.190, 0.372)
    sp.C(-0.460, 0.360, -0.800, 0.400, -1.060, 0.520)
    sp.C(-1.150, 0.620, -1.100, 0.700, -1.020, 0.740)
    sp.C(-0.760, 0.640, -0.480, 0.560, -0.300, 0.560)
    sp.C(-0.230, 0.500, -0.210, 0.430, -0.190, 0.372)
    sleeve.append(sp.pts)

    # 右袖（略收）
    sleeve2 = []
    sp2 = P(0.230, 0.372)
    sp2.C(0.400, 0.380, 0.560, 0.420, 0.640, 0.500)
    sp2.C(0.690, 0.580, 0.700, 0.640, 0.660, 0.676)
    sp2.C(0.540, 0.600, 0.420, 0.540, 0.330, 0.540)
    sp2.C(0.290, 0.480, 0.270, 0.420, 0.230, 0.372)
    sleeve2.append(sp2.pts)

    # 腰间小刀（斜）
    knife = []
    kp = P(0.30, 0.62)
    kp.C(0.44, 0.560, 0.56, 0.520, 0.64, 0.500)
    kp.C(0.56, 0.560, 0.44, 0.620, 0.30, 0.680)
    knife.append(kp.pts)

    m = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(m)
    for poly in hat + [head, neck] + body + sleeve + sleeve2:
        d.polygon(mp(poly), fill=255)
    # 刀用线画
    dd = ImageDraw.Draw(m)
    dd.line(mp(knife[0]) + [mp(knife[0])[0]], fill=255, width=max(1, int(1.6 * ss)))

    paint_alpha(canvas, color, m, alpha)
    # 脚下一抹淡墨接地
    sw = W // 8
    yy, xx = np.mgrid[0:H // 8, 0:sw].astype(np.float32)
    r = np.sqrt(((xx / sw) - 0.5) ** 2 * 1.0 + ((yy / (H // 8)) - 0.62) ** 2 * 3.0)
    a = np.clip(1.0 - r * 2.1, 0, 1) ** 1.5
    paint_alpha(canvas, mix(color, hx(PAPER), 0.35), low_mask(a * 0.55, (W, H)), 0.7)


def render_hero(ss=4):
    rng = np.random.default_rng(seed_of("hero-banner"))
    W, H = BANNER[0] * ss, BANNER[1] * ss
    pal = palette_for("indigo")

    canvas = paper_canvas(W, H, rng)

    # 天色：上暖下冷的一层极淡色场
    lw, lh = W // 8, H // 8
    yy, xx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    ny = yy / lh
    a = (1.0 - smoothstep(ny, 0.05, 0.95)) * 0.42 + 0.10
    paint_alpha(canvas, mix(hx(PAPER_HI), hx("#EFE7D9"), 0.35), low_mask(np.clip(a, 0, 1), (W, H)), 0.55)

    # 淡月
    ml = W // 8
    mr = radius_mask(ml, H // 8, 0.795, 0.245, 0.085, 0.115, power=1.4)
    paint_alpha(canvas, mix(hx(GOLD), hx(PAPER_HI), 0.62), low_mask(mr * 0.55, (W, H)), 0.55)
    # 月晕
    mr2 = radius_mask(ml, H // 8, 0.795, 0.245, 0.20, 0.28, power=1.1)
    paint_alpha(canvas, mix(hx(GOLD), hx(PAPER_HI), 0.85), low_mask(mr2 * 0.30, (W, H)), 0.5)

    # 远山四层 + 雾气
    layers = 4
    for lv in range(layers):
        draw_mountain(canvas, rng, W, H, lv, ss, layers)
        if lv < layers - 1:
            draw_mist(canvas, rng, W, H, 0.545 + 0.145 * lv, 0.028, 0.62 - 0.06 * lv, ss)

    # 人物：站在最近的山脊上（右侧 74%）
    draw_figure(canvas, 0.742, 0.845, H * 0.395, mix(hx("#1B1815"), hx("#38485A"), 0.25), 0.80, W, H, ss)

    # 飞鸟三两只
    bm = Image.new("L", (W, H), 0)
    bd = ImageDraw.Draw(bm)
    for (bx, by, bs) in [(0.545, 0.300, 1.0), (0.612, 0.268, 0.72), (0.655, 0.318, 0.55)]:
        p0 = (bx * W, by * H)
        w1 = 11 * ss * bs
        h1 = 5.5 * ss * bs
        bd.line([(p0[0] - w1, p0[1] + h1 * 0.4), (p0[0], p0[1] - h1 * 0.4), (p0[0] + w1, p0[1] + h1 * 0.4)],
                fill=255, width=max(1, int(round(1.4 * ss))), joint="curve")
    paint_alpha(canvas, mix(hx("#1B1815"), hx("#38485A"), 0.3), bm, 0.42)

    # 底部淡淡的暗，托住画面
    sh = Image.new("L", (W, H), 0)
    sh.paste(Image.new("L", (W, int(H * 0.14)), 90), (0, int(H * 0.86)))
    sh = sh.filter(ImageFilter.GaussianBlur(30 * ss))
    paint_alpha(canvas, hx(INK_SOFT), sh, 0.22)

    # 左侧留白处再提亮一档，保证标题可读
    lw2 = W // 8
    yy2, xx2 = np.mgrid[0:lh, 0:lw2].astype(np.float32)
    nx2 = xx2 / lw2
    a2 = (1.0 - smoothstep(nx2, 0.30, 0.46)) * 0.42
    paint_alpha(canvas, hx(PAPER_HI), low_mask(a2, (W, H)), 0.60)

    draw_vignette(canvas, W, H)
    out = canvas.resize(BANNER, Image.LANCZOS)
    return out


# ----------------------------------------------------------------------------
# 头像
# ----------------------------------------------------------------------------
def render_avatar(user, ss=4):
    rng = np.random.default_rng(seed_of(user["id"]))
    W, H = AVATAR[0] * ss, AVATAR[1] * ss
    pal = palette_for(user["color"])

    canvas = paper_canvas(W, H, rng)
    lw = W // 8
    yy, xx = np.mgrid[0:lw, 0:lw].astype(np.float32)
    nx, ny = xx / lw - 0.5, yy / lw - 0.5
    d = np.sqrt(nx * nx + ny * ny)

    # 圆形色场
    a = (1.0 - smoothstep(d, 0.16, 0.50)) * 0.62
    paint_alpha(canvas, pal["field"], low_mask(a, (W, H)), 0.9)
    a2 = (1.0 - smoothstep(d, 0.10, 0.40)) * 0.50
    paint_alpha(canvas, pal["field2"], low_mask(a2, (W, H)), 0.9)

    # 圆形墨晕
    acc = np.zeros((lw, lw), np.float32)
    for _ in range(3):
        cx0 = 0.5 + rng.uniform(-0.10, 0.10)
        cy0 = 0.5 + rng.uniform(-0.10, 0.10)
        rx = rng.uniform(0.20, 0.34)
        m = Image.new("L", (lw, lw), 0)
        d2 = ImageDraw.Draw(m)
        n = 22
        ang = np.linspace(0, 2 * math.pi, n, endpoint=False)
        rad = rng.uniform(0.62, 1.0, n)
        for _ in range(4):
            rad = (np.roll(rad, 1) + rad + np.roll(rad, -1)) / 3.0
        d2.polygon([(cx0 * lw + math.cos(t) * rx * lw * r, cy0 * lw + math.sin(t) * rx * lw * r)
                    for t, r in zip(ang, rad)], fill=255)
        arr = blur_np(np.asarray(m, np.float32) / 255.0, 6, 3)
        acc += arr * 0.30
    acc = np.clip(acc, 0, 0.5) * (1.0 - smoothstep(d, 0.36, 0.50))
    paint_alpha(canvas, pal["bloom"], low_mask(acc, (W, H)), 0.9)

    # 细金环
    ring = Image.new("L", (W, H), 0)
    rd = ImageDraw.Draw(ring)
    rr = W * 0.452
    rd.ellipse([W / 2 - rr, H / 2 - rr, W / 2 + rr, H / 2 + rr], outline=255,
               width=max(1, int(round(1.2 * ss))))
    paint_alpha(canvas, hx(GOLD), ring, 0.55)

    # 居中单字
    gsize = 132 * ss
    m = Image.new("L", (W, H), 0)
    d3 = ImageDraw.Draw(m)
    d3.text((W / 2, H / 2 + 2 * ss), user["glyph"], font=font(FONT_KAI, int(gsize)),
            fill=255, anchor="mm")
    paint_alpha(canvas, pal["glyph"], m, 0.86)

    # 边缘柔化：圆形外渐隐到纸色
    soft = np.clip(1.0 - smoothstep(d, 0.40, 0.505), 0.0, 1.0)
    soft_mask = low_mask(1.0 - soft, (W, H))
    paint_alpha(canvas, hx(PAPER), soft_mask, 1.0)

    out = canvas.resize(AVATAR, Image.LANCZOS)
    return out


# ----------------------------------------------------------------------------
# 输出
# ----------------------------------------------------------------------------
def save_jpeg(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.convert("RGB").save(path, "JPEG", quality=88, optimize=True, subsampling=1,
                            progressive=True)
    return os.path.getsize(path)


def targets():
    """(系列, 文件名, 渲染函数(ss) -> Image, 相对输出路径)"""
    t = []
    for it in ITEMS:
        t.append(("fashion", it["n"], lambda ss, it=it: render_fashion(it, ss), f"fashion/{it['n']}.jpg"))
    for of in OUTFITS:
        t.append(("outfit", of["n"], lambda ss, of=of: render_outfit(of, ss), f"outfit/{of['n']}.jpg"))
    t.append(("hero", "hero-banner", lambda ss: render_hero(ss), "hero-banner.jpg"))
    for u in AVATARS:
        t.append(("avatar", u["n"], lambda ss, u=u: render_avatar(u, ss), f"avatar/{u['n']}.jpg"))
    return t


def select(all_t, only):
    if not only:
        return all_t
    keys = [s.strip() for s in only.replace(",", " ").split() if s.strip()]
    out = []
    for t in all_t:
        if any(t[1] == k or t[0] == k or k == "all" for k in keys):
            out.append(t)
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description="生成《燕云十六声》古风时装视觉素材")
    ap.add_argument("--only", default="", help="只生成指定项：fashion-01 / fashion / outfit-03 / hero / avatar")
    ap.add_argument("--ss", type=int, default=4, help="超采样倍数，默认 4")
    ap.add_argument("--preview", action="store_true", help="低倍预览，写到 work/preview，不动正式文件")
    ap.add_argument("--list", action="store_true", help="列出所有可生成项")
    args = ap.parse_args(argv)

    all_t = targets()
    if args.list:
        for kind, name, _, rel in all_t:
            print(f"{kind:8s} {name:12s} {rel}")
        return 0

    jobs = select(all_t, args.only)
    if not jobs:
        print("没有匹配的生成项")
        return 1

    ss = args.ss
    dest = os.path.join(ROOT, "work", "preview") if args.preview else OUT_ROOT
    print(f"开始生成 {len(jobs)} 张（超采样 ×{ss}）→ {os.path.relpath(dest, ROOT)}")
    total = 0
    biggest = (0, "")
    t0 = time.time()
    for kind, name, fn, rel in jobs:
        st = time.time()
        img = fn(ss)
        path = os.path.join(dest, rel)
        size = save_jpeg(img, path)
        total += size
        if size > biggest[0]:
            biggest = (size, rel)
        print(f"  {rel:34s} {size/1024:7.1f} KB  {time.time()-st:5.1f}s")

    print(f"完成：{len(jobs)} 张，合计 {total/1024/1024:.2f} MB，最大 {biggest[1]} "
          f"{biggest[0]/1024:.1f} KB，用时 {time.time()-t0:.1f}s")
    return 0


if __name__ == "__main__":
    sys.exit(main())
