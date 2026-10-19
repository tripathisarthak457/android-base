import json,sys
up={e['path']:e['sha'] for e in json.load(open('up966-tree.json'))['tree'] if e['type']=='blob'}
ex={}
for line in open('ext-tree.txt',encoding='utf-8'):
    meta,path=line.rstrip('\n').split('\t',1)
    mode,typ,sha=meta.split()
    if typ=='blob': ex[path]=sha
mod=[p for p in ex if p in up and ex[p]!=up[p]]
added=[p for p in ex if p not in up]
removed=[p for p in up if p not in ex]
print('upstream blobs',len(up),'extera blobs',len(ex))
print('modified',len(mod),'added',len(added),'removed',len(removed))
open('modified.txt','w').write('\n'.join(sorted(mod))+'\n')
open('added.txt','w').write('\n'.join(sorted(added))+'\n')
open('removed.txt','w').write('\n'.join(sorted(removed))+'\n')
