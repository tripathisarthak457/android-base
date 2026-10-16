def lin(c):
    c = c / 255
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
def lum(h):
    h = h.lstrip('#')[-6:]
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
def cr(a, b):
    x, y = sorted((lum(a), lum(b)), reverse=True)
    return (x + 0.05) / (y + 0.05)
themes = {
 'light-cool': dict(bg='F7F8FA', bgg='F2F3F7', surf='FFFFFF', surfg='FFFFFF', elev='FFFFFF',
     text=dict(p='0B0F1A', s='3D4552', t='626A78'), outline='838B99'),
 'dark-cool': dict(bg='0B0E14', bgg='0B0E14', surf='141922', surfg='141922', elev='1D232E',
     text=dict(p='F2F4F8', s='B4BCCA', t='8A93A3'), outline='687183'),
 'light-paper': dict(bg='F7F4EE', bgg='F1EDE5', surf='FFFDF9', surfg='FFFDF9', elev='FFFDF9',
     text=dict(p='15130F', s='3F3931', t='655D51'), outline='8C8273'),
 'dark-paper': dict(bg='12110F', bgg='12110F', surf='1B1916', surfg='1B1916', elev='25221E',
     text=dict(p='F4EFE6', s='CBC2B4', t='A0978A'), outline='7A7164'),
 'light-cream': dict(bg='FFF7EA', bgg='FCF1DE', surf='FFFFFF', surfg='FFFFFF', elev='FFFFFF',
     text=dict(p='1A1206', s='45392A', t='6A5B43'), outline='94855F'),
 'dark-cream': dict(bg='15120D', bgg='15120D', surf='1F1A13', surfg='1F1A13', elev='2A2419',
     text=dict(p='FBF3E4', s='D6C8B0', t='A99A80'), outline='7E715A'),
}
for name, t in themes.items():
    bgs = {k: t[k] for k in ('bg', 'bgg', 'surf', 'surfg', 'elev')}
    worst = {r: min(cr(c, b) for b in bgs.values()) for r, c in t['text'].items()}
    ow = min(cr(t['outline'], b) for b in bgs.values())
    print(name, {k: round(v, 2) for k, v in worst.items()}, 'outline', round(ow, 2))
