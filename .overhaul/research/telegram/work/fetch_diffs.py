import sys, os, urllib.request, difflib, concurrent.futures as cf
SRC='D:/projects/tools/.overhaul/research/telegram/src/exteragram/'
OUT=sys.argv[1]
TAG='release-9.6.6_3362'
paths=[l.strip() for l in open('modified.txt') if l.strip().endswith('.java')]
def work(p):
    url=f'https://raw.githubusercontent.com/DrKLO/Telegram/{TAG}/{p}'
    for attempt in range(4):
        try:
            up=urllib.request.urlopen(url,timeout=60).read().decode('utf-8','replace')
            break
        except Exception as e:
            err=e
    else:
        return p,None,str(err)
    loc=open(SRC+p,encoding='utf-8',errors='replace',newline='').read()
    up=up.replace('\r\n','\n'); loc=loc.replace('\r\n','\n')
    d=list(difflib.unified_diff(up.split('\n'),loc.split('\n'),'upstream/'+p,'extera/'+p,n=3,lineterm=''))
    name=p.replace('/','__')+'.diff'
    open(os.path.join(OUT,'diffs',name),'w',encoding='utf-8').write('\n'.join(d))
    return p,len(d),None
with cf.ThreadPoolExecutor(8) as ex:
    res=list(ex.map(work,paths))
bad=[r for r in res if r[2]]
print('done',len(res),'errors',len(bad))
for r in bad[:10]: print(r)
open(os.path.join(OUT,'difflens.txt'),'w').write('\n'.join(f'{r[1]}\t{r[0]}' for r in res if r[1] is not None))
