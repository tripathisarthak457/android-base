import re, sys, urllib.request, concurrent.futures
REPOS=("https://dl.google.com/android/maven2/","https://repo1.maven.org/maven2/","https://plugins.gradle.org/m2/")
UNST=re.compile(r"(alpha|beta|rc|dev|eap|snapshot|preview|-m\d|\.m\d|-b\d)",re.I)
def key(v): return [(0,int(p)) if p.isdigit() else (1,p) for p in re.split(r"[.\-]",v)]
def look(c):
    g,a=c.split(":"); path=f"{g.replace('.','/')}/{a}/maven-metadata.xml"
    for r in REPOS:
        try:
            m=urllib.request.urlopen(r+path,timeout=20).read().decode()
        except Exception: continue
        vs=re.findall(r"<version>([^<]+)</version>",m)
        lu=re.search(r"<lastUpdated>(\d+)</lastUpdated>",m)
        st=[v for v in vs if not UNST.search(v)]
        return c, (max(st,key=key) if st else None), (max(vs,key=key) if vs else None), (lu.group(1)[:8] if lu else "?"), r.split('/')[2]
    return c,None,None,None,None
coords=[l.strip() for l in sys.stdin if l.strip() and not l.startswith('#')]
with concurrent.futures.ThreadPoolExecutor(16) as p:
    for c,s,a,lu,r in p.map(look,coords):
        print(f"{c:75} stable={s!s:14} newest={a!s:22} upd={lu} ({r})")
