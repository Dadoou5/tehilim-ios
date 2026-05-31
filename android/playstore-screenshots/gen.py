"""
Génère les 6 captures Play Store 1080×1920 pour Tehilim.

Rendu via Pillow + python-bidi pour le RTL hébreu.
Polices : Ezra SIL SR (hébreu vocalisé), Frank Ruhl Libre (hébreu titres),
Pinyon Script (latin titres), Helvetica (latin texte).
"""

from PIL import Image, ImageDraw, ImageFont
from bidi.algorithm import get_display
from pathlib import Path

# ───── Config ─────
W, H = 1080, 1920
OUT = Path("/Users/dadoou/TEHILIM/android/playstore-screenshots")
OUT.mkdir(exist_ok=True)

# Palette
BG = (250, 250, 250)
SURFACE = (255, 255, 255)
INDIGO = (30, 58, 138)         # #1E3A8A
INDIGO_SOFT = (224, 231, 255)  # #E0E7FF
PRIMARY = (17, 17, 17)
SECONDARY = (107, 114, 128)
DIVIDER = (229, 231, 235)
GREY_BG = (244, 244, 245)
GOLD = (180, 142, 60)

# Polices
FONT_DIR_REPO = "/Users/dadoou/TEHILIM/android/app/src/main/res/font"
F_EZRA = f"{FONT_DIR_REPO}/ezra_sil_sr.ttf"
F_FRANK = f"{FONT_DIR_REPO}/frank_ruhl_libre_regular.ttf"
F_PINYON = f"{FONT_DIR_REPO}/pinyon_script_regular.ttf"
F_HELV = "/System/Library/Fonts/Helvetica.ttc"
F_HELV_BOLD = "/System/Library/Fonts/Helvetica.ttc"  # uses index 1

def font(path, size, index=0):
    return ImageFont.truetype(path, size, index=index)

# Aliases — taille en px (canvas 1080 = ~3x densité xxhdpi)
def F_LATIN(size, bold=False):
    return font(F_HELV, size, index=2 if bold else 0)

def F_HE(size, sefarad=False):
    """sefarad=True → Frank Ruhl (titres), False → Ezra SIL (versets vocalisés)"""
    return font(F_FRANK if sefarad else F_EZRA, size)

def F_PINYON_(size):
    return font(F_PINYON, size)

# ───── Helpers ─────
def rrect(draw, xy, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)

def rtl(s):
    return get_display(s)

def text_w(draw, txt, fnt):
    bbox = draw.textbbox((0, 0), txt, font=fnt)
    return bbox[2] - bbox[0]

def text_h(draw, txt, fnt):
    bbox = draw.textbbox((0, 0), txt, font=fnt)
    return bbox[3] - bbox[1]

# ───── Status bar Android ─────
def status_bar(img, draw, dark_icons=True):
    color = PRIMARY if dark_icons else (255, 255, 255)
    f = F_LATIN(28, bold=True)
    # Heure à gauche
    draw.text((36, 18), "9:41", font=f, fill=color)
    # Icônes droite (signal, wifi, batterie — pictogrammes simplifiés)
    x = W - 36
    # Batterie
    bw, bh = 56, 24
    rrect(draw, (x - bw, 22, x, 22 + bh), 4, outline=color, width=2)
    draw.rectangle((x + 2, 30, x + 6, 22 + bh - 8), fill=color)
    draw.rectangle((x - bw + 4, 26, x - 4, 22 + bh - 4), fill=color)
    x -= bw + 16
    # Wifi (3 arcs symbolisés par triangle plein)
    pts = [(x, 36), (x - 28, 36), (x - 14, 14)]
    draw.polygon(pts, fill=color)
    x -= 44
    # Signal (4 barres montantes)
    for i in range(4):
        bw_ = 6
        bh_ = 8 + i * 6
        bx = x - (4 - i) * (bw_ + 4)
        draw.rectangle((bx, 38 - bh_ + 8, bx + bw_, 38 + 8), fill=color)

# ───── BottomBar 5 onglets ─────
def bottom_bar(img, draw, selected=0):
    tabs = ["Accueil", "Aujourd'hui", "Explorer", "Favoris", "Réglages"]
    # icons : cercle stylisé qui change en fonction du tab
    bar_y = H - 240  # 160px barre + 80px nav système
    # Fond
    draw.rectangle((0, bar_y, W, H - 80), fill=SURFACE)
    draw.line((0, bar_y, W, bar_y), fill=DIVIDER, width=2)
    tab_w = W // 5
    f_label = F_LATIN(22, bold=False)
    f_label_sel = F_LATIN(22, bold=True)
    for i, name in enumerate(tabs):
        cx = i * tab_w + tab_w // 2
        col = INDIGO if i == selected else SECONDARY
        # Icône abstraite : cercle ou losange
        if i == 0:  # Home — maison stylisée
            pts = [(cx, bar_y + 32), (cx - 26, bar_y + 58), (cx - 18, bar_y + 58),
                   (cx - 18, bar_y + 78), (cx + 18, bar_y + 78), (cx + 18, bar_y + 58),
                   (cx + 26, bar_y + 58)]
            if i == selected:
                draw.polygon(pts, fill=col)
            else:
                draw.polygon(pts, outline=col, width=3)
        elif i == 1:  # Aujourd'hui — soleil/calendrier carré
            r = 24
            if i == selected:
                rrect(draw, (cx - r, bar_y + 34, cx + r, bar_y + 78), 6, fill=col)
            else:
                rrect(draw, (cx - r, bar_y + 34, cx + r, bar_y + 78), 6, outline=col, width=3)
            draw.line((cx - r + 4, bar_y + 46, cx + r - 4, bar_y + 46), fill=SURFACE if i == selected else col, width=2)
        elif i == 2:  # Explorer — losange
            r = 26
            pts = [(cx, bar_y + 32), (cx + r, bar_y + 56), (cx, bar_y + 80), (cx - r, bar_y + 56)]
            if i == selected:
                draw.polygon(pts, fill=col)
            else:
                draw.polygon(pts, outline=col, width=3)
        elif i == 3:  # Favoris — étoile simplifiée
            r = 26
            pts = []
            import math
            for k in range(10):
                ang = -math.pi / 2 + k * math.pi / 5
                rr = r if k % 2 == 0 else r * 0.45
                pts.append((cx + rr * math.cos(ang), bar_y + 56 + rr * math.sin(ang)))
            if i == selected:
                draw.polygon(pts, fill=col)
            else:
                draw.polygon(pts, outline=col, width=3)
        else:  # Réglages — roue (cercle évidé)
            r = 24
            draw.ellipse((cx - r, bar_y + 32, cx + r, bar_y + 80), outline=col, width=3,
                         fill=col if i == selected else None)
            draw.ellipse((cx - 8, bar_y + 48, cx + 8, bar_y + 64), outline=col, width=3,
                         fill=SURFACE if i == selected else None)
        # Label
        f_use = f_label_sel if i == selected else f_label
        tw = text_w(draw, name, f_use)
        draw.text((cx - tw // 2, bar_y + 100), name, font=f_use, fill=col)
    # Nav bar Android (3 boutons système)
    nav_y = H - 80
    draw.rectangle((0, nav_y, W, H), fill=(245, 245, 245))
    # Retour : triangle
    cx = W // 4
    pts = [(cx - 14, nav_y + 40), (cx + 14, nav_y + 22), (cx + 14, nav_y + 58)]
    draw.polygon(pts, outline=PRIMARY, width=2)
    # Home : cercle
    cx = W // 2
    draw.ellipse((cx - 18, nav_y + 22, cx + 18, nav_y + 58), outline=PRIMARY, width=2)
    # Récents : carré
    cx = 3 * W // 4
    rrect(draw, (cx - 16, nav_y + 24, cx + 16, nav_y + 56), 3, outline=PRIMARY, width=2)

# ───── AppBar M3 ─────
def app_bar(img, draw, title, back=False, action=None):
    y0 = 60
    h = 120
    draw.rectangle((0, y0, W, y0 + h), fill=SURFACE)
    draw.line((0, y0 + h, W, y0 + h), fill=DIVIDER, width=1)
    if back:
        # Flèche gauche
        cx, cy = 60, y0 + h // 2
        draw.line((cx - 12, cy, cx + 16, cy), fill=PRIMARY, width=4)
        draw.line((cx - 12, cy, cx, cy - 14), fill=PRIMARY, width=4)
        draw.line((cx - 12, cy, cx, cy + 14), fill=PRIMARY, width=4)
        tx = 120
    else:
        tx = 48
    f = F_LATIN(44, bold=True)
    draw.text((tx, y0 + h // 2 - text_h(draw, title, f) // 2 - 6), title, font=f, fill=PRIMARY)
    if action:
        # Petite pastille à droite
        f_a = F_LATIN(26, bold=False)
        aw = text_w(draw, action, f_a) + 32
        rrect(draw, (W - aw - 36, y0 + h // 2 - 22, W - 36, y0 + h // 2 + 22), 20,
              fill=INDIGO_SOFT)
        draw.text((W - aw - 20, y0 + h // 2 - text_h(draw, action, f_a) // 2 - 4),
                  action, font=f_a, fill=INDIGO)

# ───── 1. HOME ─────
def screen_home():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d)

    # Pas d'AppBar standard, juste un grand titre + date
    y = 200
    # Date FR
    f = F_LATIN(28)
    d.text((48, y), "Dimanche · 24 Iyar 5786", font=f, fill=SECONDARY)
    y += 44
    # Date HE alignée gauche, RTL via bidi
    he_date = "כ״ד באייר ה׳תשפ״ו"
    f_he = F_HE(36)
    d.text((48, y), rtl(he_date), font=f_he, fill=SECONDARY)
    y += 80

    # Carte "Reprendre la lecture"
    card_h = 220
    rrect(d, (48, y, W - 48, y + card_h), 20, fill=SURFACE)
    d.rectangle((48, y, 60, y + card_h), fill=INDIGO)  # bande accent
    f_label = F_LATIN(22, bold=True)
    d.text((84, y + 28), "REPRENDRE LA LECTURE", font=f_label, fill=INDIGO)
    f_title = F_LATIN(48, bold=True)
    d.text((84, y + 64), "Tehilim 23 · Verset 4", font=f_title, fill=PRIMARY)
    f_he_q = F_HE(34)
    quote = "גַּם כִּי־אֵלֵךְ בְּגֵיא צַלְמָוֶת"
    d.text((84, y + 134), rtl(quote), font=f_he_q, fill=PRIMARY)
    f_fr_q = F_LATIN(24)
    d.text((84, y + 180), "« Même si je marche dans la vallée... »",
           font=f_fr_q, fill=SECONDARY)
    y += card_h + 60

    # Section "Tehilim du jour"
    f_sec = F_LATIN(26, bold=True)
    d.text((48, y), "TEHILIM DU JOUR", font=f_sec, fill=SECONDARY)
    f_count = F_LATIN(22)
    d.text((W - 48 - text_w(d, "5 chapitres", f_count), y + 4),
           "5 chapitres", font=f_count, fill=SECONDARY)
    y += 56
    # 5 cards horizontales
    pad = 16
    card_w = (W - 96 - 4 * pad) // 5
    card_hh = 160
    nums = [(24, "כ״ד"), (25, "כ״ה"), (26, "כ״ו"), (27, "כ״ז"), (28, "כ״ח")]
    for i, (n, he) in enumerate(nums):
        x = 48 + i * (card_w + pad)
        rrect(d, (x, y, x + card_w, y + card_hh), 16,
              fill=INDIGO if i == 0 else SURFACE)
        f_n = F_LATIN(46, bold=True)
        col_n = SURFACE if i == 0 else PRIMARY
        col_h = INDIGO_SOFT if i == 0 else SECONDARY
        nw = text_w(d, str(n), f_n)
        d.text((x + card_w // 2 - nw // 2, y + 28), str(n), font=f_n, fill=col_n)
        f_he_s = F_HE(32, sefarad=True)
        hw = text_w(d, rtl(he), f_he_s)
        d.text((x + card_w // 2 - hw // 2, y + 86), rtl(he), font=f_he_s, fill=col_h)
    y += card_hh + 56

    # Section Explorer
    d.text((48, y), "EXPLORER", font=f_sec, fill=SECONDARY)
    y += 56
    tiles = [
        ("5 Livres", "150 Tehilim"),
        ("Cas de la vie", "17 thèmes"),
        ("Lelouy Nichmat", "Séquences"),
    ]
    tile_w = (W - 96 - 2 * pad) // 3
    tile_h = 200
    for i, (name, sub) in enumerate(tiles):
        x = 48 + i * (tile_w + pad)
        rrect(d, (x, y, x + tile_w, y + tile_h), 16, fill=SURFACE)
        # Icône abstraite : carré indigo en haut-gauche
        rrect(d, (x + 20, y + 20, x + 76, y + 76), 12, fill=INDIGO_SOFT)
        rrect(d, (x + 32, y + 32, x + 64, y + 64), 6, fill=INDIGO)
        f_t = F_LATIN(28, bold=True)
        # Wrap si trop large
        if text_w(d, name, f_t) > tile_w - 40:
            words = name.split()
            mid = len(words) // 2
            line1 = " ".join(words[:mid]) if mid else words[0]
            line2 = " ".join(words[mid:]) if mid else (words[1] if len(words) > 1 else "")
            d.text((x + 20, y + 100), line1, font=f_t, fill=PRIMARY)
            d.text((x + 20, y + 134), line2, font=f_t, fill=PRIMARY)
        else:
            d.text((x + 20, y + 100), name, font=f_t, fill=PRIMARY)
        f_s = F_LATIN(20)
        d.text((x + 20, y + 168), sub, font=f_s, fill=SECONDARY)

    bottom_bar(img, d, selected=0)
    img.save(OUT / "01_home.png", optimize=True)

# ───── 2. PSALM DETAIL — Tehilim 23 ─────
def screen_psalm_detail():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d)
    app_bar(img, d, "Tehilim 23", back=True, action="Aa")

    y = 200
    # Titre hébreu grand centré
    title_he = "מִזְמוֹר לְדָוִד"
    f_t = F_HE(96)
    tw = text_w(d, rtl(title_he), f_t)
    d.text((W // 2 - tw // 2, y), rtl(title_he), font=f_t, fill=PRIMARY)
    y += 130
    # Sous-titre pinyon
    f_p = F_PINYON_(68)
    pw = text_w(d, "Psaume de David", f_p)
    d.text((W // 2 - pw // 2, y), "Psaume de David", font=f_p, fill=GOLD)
    y += 110
    # Divider doré centré
    d.rectangle((W // 2 - 80, y, W // 2 + 80, y + 2), fill=GOLD)
    y += 40

    # Versets
    verses = [
        ("א", "יְהֹוָה רֹעִי לֹא אֶחְסָר",
         "L'Éternel est mon berger, je ne manquerai de rien."),
        ("ב", "בִּנְאוֹת דֶּשֶׁא יַרְבִּיצֵנִי עַל־מֵי מְנֻחוֹת יְנַהֲלֵנִי",
         "Il me fait reposer dans de verts pâturages."),
        ("ג", "נַפְשִׁי יְשׁוֹבֵב יַנְחֵנִי בְמַעְגְּלֵי־צֶדֶק",
         "Il restaure mon âme, me guide sur les sentiers de la justice."),
        ("ד", "גַּם כִּי־אֵלֵךְ בְּגֵיא צַלְמָוֶת לֹא־אִירָא רָע",
         "Même si je marche dans la vallée de l'ombre de la mort..."),
    ]
    f_num = F_LATIN(34, bold=True)
    f_num_he = F_HE(38, sefarad=True)
    f_he_v = F_HE(40)
    f_fr_v = F_LATIN(26)
    for he_num, he, fr in verses:
        # Pastille numéro hébreu à droite (RTL)
        nw = text_w(d, rtl(he_num), f_num_he)
        d.ellipse((W - 96, y, W - 40, y + 56), fill=INDIGO_SOFT)
        d.text((W - 68 - nw // 2, y + 8), rtl(he_num), font=f_num_he, fill=INDIGO)
        # Hébreu RTL — aligné droite
        he_disp = rtl(he)
        hw = text_w(d, he_disp, f_he_v)
        d.text((W - 124 - hw, y + 6), he_disp, font=f_he_v, fill=PRIMARY)
        y += 64
        # Traduction FR alignée gauche
        d.text((60, y), fr, font=f_fr_v, fill=SECONDARY)
        y += 50
        # Divider
        d.line((60, y, W - 60, y), fill=DIVIDER, width=1)
        y += 24

    # FAB favoris en bas à droite (au-dessus de la BottomBar)
    fab_cx, fab_cy = W - 110, H - 320
    d.ellipse((fab_cx - 50, fab_cy - 50, fab_cx + 50, fab_cy + 50), fill=INDIGO)
    # Cœur stylisé
    import math
    pts = []
    for k in range(60):
        t = k * 2 * math.pi / 60
        x_ = 16 * math.sin(t) ** 3
        y_ = -(13 * math.cos(t) - 5 * math.cos(2*t) - 2 * math.cos(3*t) - math.cos(4*t))
        pts.append((fab_cx + x_, fab_cy + y_ - 4))
    d.polygon(pts, outline=SURFACE, width=3)

    bottom_bar(img, d, selected=2)
    img.save(OUT / "02_psalm_detail.png", optimize=True)

# ───── 3. TEHILIM 119 GRID ─────
def screen_119():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d)
    app_bar(img, d, "Tehilim 119", back=True)

    y = 220
    f_sub = F_LATIN(28)
    d.text((48, y), "Par lettre de l'alphabet · 22 sections de 8 versets",
           font=f_sub, fill=SECONDARY)
    y += 80

    alphabet = ["א", "ב", "ג", "ד", "ה", "ו", "ז", "ח",
                "ט", "י", "כ", "ל", "מ", "נ", "ס", "ע",
                "פ", "צ", "ק", "ר", "ש", "ת"]
    names = ["Aleph", "Beth", "Gimel", "Daleth", "Hé", "Vav", "Zayin", "Heth",
             "Tet", "Youd", "Kaph", "Lamed", "Mem", "Noun", "Samekh", "Ayin",
             "Pé", "Tsadé", "Qof", "Rech", "Chin", "Tav"]

    cols = 4
    pad = 20
    cell_w = (W - 96 - (cols - 1) * pad) // cols
    cell_h = cell_w
    f_letter = F_HE(120, sefarad=True)
    f_name = F_LATIN(22, bold=True)
    f_range = F_LATIN(20)

    # V1.4 — sens de lecture hébreu : Aleph en haut à droite. On construit
    # une grille `cells` de longueur multiple de `cols`, chaque ligne inversée,
    # la dernière (incomplète) préfixée de placeholders à gauche.
    cells = []  # liste de tuples (letter, name, range_str) ou None pour placeholder
    i = 0
    n_total = len(alphabet)
    while i < n_total:
        end = min(i + cols, n_total)
        chunk = list(zip(alphabet[i:end], names[i:end], range(i, end)))
        padding = cols - len(chunk)
        cells.extend([None] * padding)  # placeholders à gauche
        cells.extend(reversed(chunk))   # sections inversées
        i = end

    for pos, cell in enumerate(cells):
        row, col = pos // cols, pos % cols
        x = 48 + col * (cell_w + pad)
        yy = y + row * (cell_h + pad)
        if cell is None:
            continue
        l, n, idx = cell
        rrect(d, (x, yy, x + cell_w, yy + cell_h), 18, fill=SURFACE)
        # Lettre centrée
        lw = text_w(d, l, f_letter)
        d.text((x + cell_w // 2 - lw // 2, yy + cell_h // 2 - 100), l,
               font=f_letter, fill=INDIGO)
        # Nom translittéré
        nw = text_w(d, n, f_name)
        d.text((x + cell_w // 2 - nw // 2, yy + cell_h - 70), n,
               font=f_name, fill=PRIMARY)
        # Range — utilise idx (position dans alphabet), pas i (compteur chunking)
        r = f"v. {idx*8+1}-{idx*8+8}"
        rw = text_w(d, r, f_range)
        d.text((x + cell_w // 2 - rw // 2, yy + cell_h - 38), r,
               font=f_range, fill=SECONDARY)

    bottom_bar(img, d, selected=2)
    img.save(OUT / "03_tehilim_119.png", optimize=True)

# ───── 4. LIFE CASES ─────
def screen_life_cases():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d)
    app_bar(img, d, "Cas de la vie", back=True)

    # Icône = forme géométrique dessinée (heart, star, cross, etc.)
    sections = [
        ("CYCLE DE VIE", [
            ("Naissance d'un enfant", "3 Tehilim", "dot"),
            ("Mariage", "5 Tehilim", "heart"),
            ("Période de deuil", "7 Tehilim", "diamond"),
        ]),
        ("SANTÉ ET ÉPREUVES", [
            ("Guérison d'un malade", "8 Tehilim", "cross"),
            ("Protection en voyage", "4 Tehilim", "triangle"),
        ]),
        ("SPIRITUALITÉ", [
            ("Étude de la Torah", "6 Tehilim", "magen"),
            ("Téchouva — repentir", "5 Tehilim", "arrow"),
        ]),
    ]

    def draw_icon(d, kind, cx, cy, r):
        import math
        if kind == "dot":
            d.ellipse((cx - r * 0.5, cy - r * 0.5, cx + r * 0.5, cy + r * 0.5), fill=INDIGO)
        elif kind == "heart":
            pts = []
            for k in range(80):
                t = k * 2 * math.pi / 80
                x_ = 16 * math.sin(t) ** 3
                y_ = -(13 * math.cos(t) - 5 * math.cos(2*t) - 2 * math.cos(3*t) - math.cos(4*t))
                pts.append((cx + x_ * r / 18, cy + y_ * r / 18 - r * 0.05))
            d.polygon(pts, fill=INDIGO)
        elif kind == "diamond":
            pts = [(cx, cy - r * 0.7), (cx + r * 0.7, cy), (cx, cy + r * 0.7), (cx - r * 0.7, cy)]
            d.polygon(pts, fill=INDIGO)
        elif kind == "cross":
            tw = r * 0.32
            d.rectangle((cx - tw, cy - r * 0.7, cx + tw, cy + r * 0.7), fill=INDIGO)
            d.rectangle((cx - r * 0.7, cy - tw, cx + r * 0.7, cy + tw), fill=INDIGO)
        elif kind == "triangle":
            pts = [(cx, cy - r * 0.75), (cx + r * 0.7, cy + r * 0.5), (cx - r * 0.7, cy + r * 0.5)]
            d.polygon(pts, fill=INDIGO)
        elif kind == "magen":  # Étoile de David — deux triangles superposés
            up = [(cx, cy - r * 0.75), (cx + r * 0.7, cy + r * 0.4), (cx - r * 0.7, cy + r * 0.4)]
            dn = [(cx, cy + r * 0.75), (cx + r * 0.7, cy - r * 0.4), (cx - r * 0.7, cy - r * 0.4)]
            d.polygon(up, outline=INDIGO, width=4)
            d.polygon(dn, outline=INDIGO, width=4)
        elif kind == "arrow":  # Flèche circulaire (téchouva = retour)
            d.arc((cx - r * 0.7, cy - r * 0.7, cx + r * 0.7, cy + r * 0.7),
                  start=30, end=330, fill=INDIGO, width=5)
            # Pointe de flèche
            ax, ay = cx + r * 0.7 * math.cos(math.radians(30)), cy + r * 0.7 * math.sin(math.radians(30))
            pts = [(ax, ay), (ax - r * 0.25, ay - r * 0.05), (ax - r * 0.15, ay + r * 0.25)]
            d.polygon(pts, fill=INDIGO)

    y = 200
    f_section = F_LATIN(24, bold=True)
    f_title = F_LATIN(34, bold=True)
    f_sub = F_LATIN(24)

    for sec_name, items in sections:
        y += 24
        d.text((48, y), sec_name, font=f_section, fill=SECONDARY)
        y += 50
        # Group container
        for j, (title, sub, ic) in enumerate(items):
            row_h = 130
            # Cercle pastel + icône géométrique centrée
            cx, cy = 111, y + 18 + 43
            d.ellipse((68, y + 18, 154, y + 18 + 86), fill=INDIGO_SOFT)
            draw_icon(d, ic, cx, cy, 32)
            # Titre
            d.text((192, y + 28), title, font=f_title, fill=PRIMARY)
            # Sub
            d.text((192, y + 76), sub, font=f_sub, fill=SECONDARY)
            # Chevron droit
            cx = W - 80
            cy = y + row_h // 2
            d.line((cx - 8, cy - 12, cx + 4, cy), fill=SECONDARY, width=4)
            d.line((cx + 4, cy, cx - 8, cy + 12), fill=SECONDARY, width=4)
            # Divider entre items (sauf dernier)
            if j < len(items) - 1:
                d.line((192, y + row_h, W - 48, y + row_h), fill=DIVIDER, width=1)
            y += row_h
        # Container background derrière toute la section (par-dessus, refait au-dessus)
        # On préfère dessiner le fond AVANT mais comme on a déjà dessiné, on le saute.
        # Visuel suffisant tel quel.
        y += 20

    bottom_bar(img, d, selected=2)
    img.save(OUT / "04_life_cases.png", optimize=True)

# ───── 5. SETTINGS ─────
def screen_settings():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d)
    app_bar(img, d, "Réglages", back=False)

    sections = [
        ("AFFICHAGE", [
            ("Thème", "Système"),
            ("Taille hébreu", "Moyen"),
            ("Taille traduction", "Moyen"),
        ]),
        ("LECTURE", [
            ("Mode du texte", "Hébreu + Traduction"),
            ("Phonétique sépharade", "Désactivé"),
            ("Numérotation", "Lettres hébraïques"),
        ]),
        ("NOTIFICATIONS", [
            ("Rappel quotidien", "21:00"),
            ("Jours actifs", "Tous les jours"),
        ]),
        ("LANGUE", [
            ("Langue de l'app", "Français"),
        ]),
    ]

    y = 220
    f_section = F_LATIN(24, bold=True)
    f_label = F_LATIN(30)
    f_value = F_LATIN(28)
    for sec_name, items in sections:
        d.text((48, y), sec_name, font=f_section, fill=SECONDARY)
        y += 44
        # Container blanc rounded
        cy0 = y
        row_h = 96
        ch = row_h * len(items)
        rrect(d, (48, cy0, W - 48, cy0 + ch), 16, fill=SURFACE)
        for j, (label, value) in enumerate(items):
            ry = cy0 + j * row_h
            d.text((72, ry + row_h // 2 - text_h(d, label, f_label) // 2 - 4),
                   label, font=f_label, fill=PRIMARY)
            vw = text_w(d, value, f_value)
            chev_w = 24
            d.text((W - 96 - vw - chev_w, ry + row_h // 2 - text_h(d, value, f_value) // 2 - 4),
                   value, font=f_value, fill=SECONDARY)
            # Chevron droit
            cx = W - 80
            cyy = ry + row_h // 2
            d.line((cx - 6, cyy - 10, cx + 4, cyy), fill=SECONDARY, width=3)
            d.line((cx + 4, cyy, cx - 6, cyy + 10), fill=SECONDARY, width=3)
            if j < len(items) - 1:
                d.line((72, ry + row_h, W - 72, ry + row_h), fill=DIVIDER, width=1)
        y = cy0 + ch + 40

    bottom_bar(img, d, selected=4)
    img.save(OUT / "05_settings.png", optimize=True)

# ───── 6. PHONETIC MODE ─────
def screen_phonetic():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d)
    app_bar(img, d, "Tehilim 1", back=True, action="Phonétique")

    y = 200
    # Titre hébreu
    title_he = "אַשְׁרֵי הָאִישׁ"
    f_t = F_HE(88)
    tw = text_w(d, rtl(title_he), f_t)
    d.text((W // 2 - tw // 2, y), rtl(title_he), font=f_t, fill=PRIMARY)
    y += 116
    # Translittération
    f_trans = F_PINYON_(60)
    txt = "Achré ha-ich"
    pw = text_w(d, txt, f_trans)
    d.text((W // 2 - pw // 2, y), txt, font=f_trans, fill=GOLD)
    y += 90
    # Divider doré
    d.rectangle((W // 2 - 60, y, W // 2 + 60, y + 2), fill=GOLD)
    y += 36

    verses = [
        ("א",
         "אַשְׁרֵי־הָאִישׁ אֲשֶׁר לֹא הָלַךְ בַּעֲצַת רְשָׁעִים",
         "Achré ha-ich acher lo halakh ba'atsat rechaïm",
         "Heureux l'homme qui ne suit pas le conseil des méchants,"),
        ("ב",
         "כִּי אִם בְּתוֹרַת יְהֹוָה חֶפְצוֹ",
         "Ki im be-Torat Adonaï heftso",
         "mais qui trouve son plaisir dans la Torah de l'Éternel,"),
        ("ג",
         "וְהָיָה כְּעֵץ שָׁתוּל עַל־פַּלְגֵי מָיִם",
         "Ve-haya ke'etz chatoul al palguei mayim",
         "Il sera comme un arbre planté près des cours d'eau,"),
    ]

    f_num_he = F_HE(36, sefarad=True)
    f_he_v = F_HE(38)
    f_phon = F_LATIN(28, bold=False)
    f_fr = F_LATIN(26)
    for he_num, he, phon, fr in verses:
        # Pastille
        d.ellipse((W - 96, y, W - 40, y + 56), fill=INDIGO_SOFT)
        nw = text_w(d, rtl(he_num), f_num_he)
        d.text((W - 68 - nw // 2, y + 8), rtl(he_num), font=f_num_he, fill=INDIGO)
        # Hébreu RTL
        he_disp = rtl(he)
        hw = text_w(d, he_disp, f_he_v)
        d.text((W - 124 - hw, y + 8), he_disp, font=f_he_v, fill=PRIMARY)
        y += 70
        # Phonétique en italique simulée (couleur indigo)
        d.text((60, y), phon, font=f_phon, fill=INDIGO)
        y += 46
        # Traduction
        d.text((60, y), fr, font=f_fr, fill=SECONDARY)
        y += 50
        d.line((60, y, W - 60, y), fill=DIVIDER, width=1)
        y += 30

    bottom_bar(img, d, selected=2)
    img.save(OUT / "06_phonetic.png", optimize=True)

# ───── 7. Play Store ICON 512×512 ─────
def icon_512():
    """Compose le foreground 432×432 (harpe + texte feu) sur le fond #0E1B3D."""
    BG_NAVY = (14, 27, 61)  # #0E1B3D
    SRC_FG = "/Users/dadoou/TEHILIM/android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"
    fg = Image.open(SRC_FG).convert("RGBA")
    # Upscale 432 → 512 (Lanczos)
    fg = fg.resize((512, 512), Image.LANCZOS)
    canvas = Image.new("RGB", (512, 512), BG_NAVY)
    canvas.paste(fg, (0, 0), fg)
    canvas.save(OUT / "playstore_icon_512.png", optimize=True)

# ───── 8. FEATURE GRAPHIC 1024×500 ─────
def feature_graphic():
    """
    Bannière Play Store horizontale.
    Layout :
      - Fond bleu nuit dégradé (#0E1B3D → #1E3A8A)
      - Côté gauche : grand titre hébreu "תְּהִלִּים" + sous-titre "Psaumes de David"
      - Côté droit : icône (harpe) reprise depuis le foreground
      - Filet doré horizontal
    """
    FW, FH = 1024, 500
    BG_DARK = (14, 27, 61)
    BG_LIGHT = (30, 58, 138)
    GOLD_ = (200, 162, 80)
    WHITE = (245, 245, 240)
    SOFT = (180, 192, 220)

    img = Image.new("RGB", (FW, FH), BG_DARK)
    d = ImageDraw.Draw(img)

    # Dégradé radial à droite (lumière indigo)
    cx, cy = int(FW * 0.78), FH // 2
    max_r = int(((FW * 0.6) ** 2 + (FH * 0.6) ** 2) ** 0.5)
    for r in range(max_r, 0, -8):
        t = 1 - r / max_r
        col = (
            int(BG_DARK[0] + (BG_LIGHT[0] - BG_DARK[0]) * t * 0.35),
            int(BG_DARK[1] + (BG_LIGHT[1] - BG_DARK[1]) * t * 0.35),
            int(BG_DARK[2] + (BG_LIGHT[2] - BG_DARK[2]) * t * 0.35),
        )
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=col)

    # Icône à droite (harpe + texte feu, repris du foreground)
    SRC_FG = "/Users/dadoou/TEHILIM/android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"
    icon = Image.open(SRC_FG).convert("RGBA")
    icon_sz = 360
    icon = icon.resize((icon_sz, icon_sz), Image.LANCZOS)
    # Cadre carré arrondi navy autour
    badge = Image.new("RGBA", (icon_sz + 40, icon_sz + 40), (0, 0, 0, 0))
    bd = ImageDraw.Draw(badge)
    rrect(bd, (0, 0, icon_sz + 40, icon_sz + 40), 60, fill=BG_DARK + (255,))
    badge.paste(icon, (20, 20), icon)
    bx = FW - icon_sz - 100
    by = (FH - icon_sz - 40) // 2
    img.paste(badge, (bx, by), badge)
    # Anneau doré subtil
    d.rounded_rectangle((bx - 4, by - 4, bx + icon_sz + 44, by + icon_sz + 44),
                        radius=64, outline=GOLD_, width=2)

    # Côté gauche : titre hébreu + tagline
    # Titre hébreu "תְּהִלִּים" (Tehilim)
    f_he_title = font(F_FRANK, 180)
    he_title = "תְּהִלִּים"
    he_disp = rtl(he_title)
    d.text((80, 100), he_disp, font=f_he_title, fill=WHITE)

    # Filet doré
    d.rectangle((80, 305, 280, 308), fill=GOLD_)

    # Sous-titre latin
    f_sub = F_LATIN(46, bold=True)
    d.text((80, 330), "Psaumes de David", font=f_sub, fill=WHITE)
    f_tag = F_LATIN(28)
    d.text((80, 396), "150 psaumes · hébreu vocalisé · hors-ligne", font=f_tag, fill=SOFT)

    img.save(OUT / "playstore_feature_graphic_1024x500.png", optimize=True)

# ───── Run ─────
if __name__ == "__main__":
    print("Generating screenshots...")
    screen_home()
    print("  ✓ 01_home.png")
    screen_psalm_detail()
    print("  ✓ 02_psalm_detail.png")
    screen_119()
    print("  ✓ 03_tehilim_119.png")
    screen_life_cases()
    print("  ✓ 04_life_cases.png")
    screen_settings()
    print("  ✓ 05_settings.png")
    screen_phonetic()
    print("  ✓ 06_phonetic.png")
    print("Generating Play Store icon + feature graphic...")
    icon_512()
    print("  ✓ playstore_icon_512.png")
    feature_graphic()
    print("  ✓ playstore_feature_graphic_1024x500.png")
    print(f"\nAll assets saved to {OUT}/")
