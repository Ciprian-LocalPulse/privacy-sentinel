#!/usr/bin/env python3
"""Builds docs/Sentinel-Whitepaper.pdf from docs/WHITEPAPER.md.

Requires: reportlab, and the DejaVu fonts (fonts-dejavu on Debian/Ubuntu).
Usage:    python3 docs/build_whitepaper.py
"""
import os
import re
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, Frame, HRFlowable, Image,
                                KeepTogether, ListFlowable, ListItem,
                                PageTemplate, Paragraph, Spacer, Table,
                                TableStyle, Preformatted)

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "WHITEPAPER.md")
OUT = os.path.join(HERE, "Sentinel-Whitepaper.pdf")
FONT_DIR = "/usr/share/fonts/truetype/dejavu"

pdfmetrics.registerFont(TTFont("DV", f"{FONT_DIR}/DejaVuSans.ttf"))
pdfmetrics.registerFont(TTFont("DV-B", f"{FONT_DIR}/DejaVuSans-Bold.ttf"))
pdfmetrics.registerFont(TTFont("DV-I", f"{FONT_DIR}/DejaVuSans-Oblique.ttf"))
pdfmetrics.registerFont(TTFont("DV-BI", f"{FONT_DIR}/DejaVuSans-BoldOblique.ttf"))
pdfmetrics.registerFont(TTFont("DVM", f"{FONT_DIR}/DejaVuSansMono.ttf"))
pdfmetrics.registerFontFamily("DV", normal="DV", bold="DV-B", italic="DV-I", boldItalic="DV-BI")

NAVY = colors.HexColor("#1E3A5F")
BLUE = colors.HexColor("#27508A")
LINE = colors.HexColor("#B8C9DE")
ZEBRA = colors.HexColor("#F1F5FA")

body = ParagraphStyle("body", fontName="DV", fontSize=9.5, leading=14.2, textColor=colors.HexColor("#1A1A1A"), alignment=TA_LEFT, spaceAfter=6)
h1 = ParagraphStyle("h1", parent=body, fontName="DV-B", fontSize=20, leading=25, textColor=NAVY, spaceBefore=6, spaceAfter=8)
h2 = ParagraphStyle("h2", parent=body, fontName="DV-B", fontSize=13.5, leading=18, textColor=NAVY, spaceBefore=14, spaceAfter=6, keepWithNext=1)
h3 = ParagraphStyle("h3", parent=body, fontName="DV-B", fontSize=10.8, leading=15, textColor=BLUE, spaceBefore=9, spaceAfter=4, keepWithNext=1)
cell = ParagraphStyle("cell", parent=body, fontSize=8.2, leading=11, spaceAfter=0)
cell_h = ParagraphStyle("cell_h", parent=cell, fontName="DV-B", textColor=colors.white)
caption = ParagraphStyle("caption", parent=body, fontName="DV-I", fontSize=8.5, textColor=colors.HexColor("#555555"), spaceAfter=10)
meta = ParagraphStyle("meta", parent=body, fontSize=9, leading=13.5, textColor=colors.HexColor("#333333"))

def inline(text: str) -> str:
    spans = []
    def stash(m):
        spans.append(m.group(1))
        return f"\x00{len(spans) - 1}\x00"
    text = re.sub(r"`([^`]+)`", stash, text)
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r"\[([^\]]+)\]\((https?://[^)]+)\)", r'<a href="\2" color="#27508A">\1</a>', text)
    text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    text = re.sub(r"(?<![\*\w])\*(?!\s)(.+?)(?<!\s)\*(?![\*\w])", r"<i>\1</i>", text)
    def unstash(m):
        code = spans[int(m.group(1))].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        return f'<font name="DVM" size="8">{code}</font>'
    return re.sub(r"\x00(\d+)\x00", unstash, text)

def make_table(rows):
    header, data = rows[0], rows[1:]
    ncols = len(header)
    weights = []
    for c in range(ncols):
        longest = max(len(r[c]) if c < len(r) else 0 for r in rows)
        weights.append(min(max(longest, 10), 55))
    total_w = A4[0] - 4 * cm
    col_w = [total_w * w / sum(weights) for w in weights]
    tbl_data = [[Paragraph(inline(h), cell_h) for h in header]]
    for r in data:
        r = r + [""] * (ncols - len(r))
        tbl_data.append([Paragraph(inline(c), cell) for c in r])
    t = Table(tbl_data, colWidths=col_w, repeatRows=1)
    style = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]
    for i in range(1, len(tbl_data)):
        if i % 2 == 0:
            style.append(("BACKGROUND", (0, i), (-1, i), ZEBRA))
    t.setStyle(TableStyle(style))
    return t

def parse(md: str):
    lines = md.splitlines()
    story, i, fig = [], 0, 0
    # cover banner
    banner = os.path.join(HERE, "images", "banner.png")
    if os.path.exists(banner):
        w = A4[0] - 4 * cm
        story += [Image(banner, width=w, height=w / 2), Spacer(1, 10)]
    while i < len(lines):
        line = lines[i]
        s = line.strip()
        if not s:
            i += 1
            continue
        if s == "---":
            story.append(HRFlowable(width="100%", thickness=0.6, color=LINE, spaceBefore=4, spaceAfter=8))
            i += 1
        elif s.startswith("### "):
            story.append(Paragraph(inline(s[4:]), h3)); i += 1
        elif s.startswith("## "):
            story.append(Paragraph(inline(s[3:]), h2)); i += 1
        elif s.startswith("# "):
            story.append(Paragraph(inline(s[2:]), h1)); i += 1
        elif s.startswith("!["):
            m = re.match(r"!\[(.*?)\]\((.*?)\)", s)
            path = os.path.join(HERE, m.group(2))
            from PIL import Image as PILImage
            with PILImage.open(path) as im:
                iw, ih = im.size
            w = A4[0] - 4 * cm
            fig += 1
            block = [Image(path, width=w, height=w * ih / iw),
                     Paragraph(f"Figure {fig}. {inline(m.group(1))}", caption)]
            # keep a preceding heading together with its figure
            if story and isinstance(story[-1], Paragraph) and story[-1].style.name in ("h2", "h3"):
                block.insert(0, story.pop())
            story.append(KeepTogether(block))
            i += 1
        elif s.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-{2,}:?", c) for c in cells):
                    rows.append(cells)
                i += 1
            story += [make_table(rows), Spacer(1, 8)]
        elif re.match(r"^(\d+)\. ", s) or s.startswith("- "):
            ordered = bool(re.match(r"^\d+\. ", s))
            items = []
            while i < len(lines) and (re.match(r"^\d+\. ", lines[i].strip()) or lines[i].strip().startswith("- ")):
                txt = re.sub(r"^(\d+\.|-) ", "", lines[i].strip())
                i += 1
                while i < len(lines) and lines[i].startswith("   ") and lines[i].strip():
                    txt += " " + lines[i].strip(); i += 1
                items.append(ListItem(Paragraph(inline(txt), body), leftIndent=14))
            story.append(ListFlowable(items, bulletType="1" if ordered else "bullet",
                                      bulletFontName="DV", bulletFontSize=8.5 if not ordered else 9,
                                      start=1 if ordered else "•", leftIndent=14))
            story.append(Spacer(1, 3))
        else:
            para = [s]
            i += 1
            while i < len(lines) and lines[i].strip() and not re.match(r"^(#|\||- |\d+\. |!\[|---)", lines[i].strip()):
                para.append(lines[i].strip()); i += 1
            joiner = "<br/>" if para[0].startswith("**Whitepaper") else " "
            style = meta if para[0].startswith("**Whitepaper") else body
            story.append(Paragraph(joiner.join(inline(p) for p in para), style))
    return story

def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("DV", 7.5)
    canvas.setFillColor(colors.HexColor("#666666"))
    canvas.drawString(2 * cm, 1.2 * cm, "Sentinel Whitepaper v1.0  ·  MIT License")
    canvas.drawRightString(A4[0] - 2 * cm, 1.2 * cm, f"Page {doc.page}")
    canvas.setStrokeColor(LINE)
    canvas.line(2 * cm, 1.6 * cm, A4[0] - 2 * cm, 1.6 * cm)
    canvas.restoreState()

doc = BaseDocTemplate(OUT, pagesize=A4, leftMargin=2 * cm, rightMargin=2 * cm, topMargin=1.8 * cm, bottomMargin=2.2 * cm,
                      title="Sentinel: A Transparent, Offline Watchdog for Camera and Microphone Use on Android",
                      author="Ciprian-LocalPulse", subject="Whitepaper v1.0", keywords="android, privacy, camera, microphone")
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="f")
doc.addPageTemplates([PageTemplate(id="p", frames=[frame], onPage=footer)])
doc.build(parse(open(SRC, encoding="utf-8").read()))
print("wrote", OUT)
