import json,sys,urllib.request
def text(node):
    out=[]
    def walk(n):
        if isinstance(n,dict):
            t=n.get('type')
            if t=='text': out.append(n.get('text',''))
            elif t=='codeVoice': out.append('`'+n.get('code','')+'`')
            elif t=='codeListing': out.append('\n```\n'+'\n'.join(n.get('code',[]))+'\n```\n')
            elif t=='heading': out.append('\n## '+n.get('text','')+'\n')
            elif t=='reference': out.append('['+n.get('identifier','').split('/')[-1]+']')
            for k in ('inlineContent','content','items'):
                if k in n: walk(n[k])
            if t=='paragraph': out.append('\n')
        elif isinstance(n,list):
            for x in n: walk(x)
    walk(node); return ''.join(out)
url=sys.argv[1]
d=json.load(urllib.request.urlopen(url,timeout=30))
md=d.get('metadata',{})
print('TITLE',md.get('title'),'| platforms',[(p.get('name'),p.get('introducedAt'),p.get('beta')) for p in md.get('platforms',[]) or []])
print(text(d.get('abstract',[])))
print(text(d.get('primaryContentSections',[])))
for s in d.get('topicSections',[]) or []:
    print('TOPIC',s.get('title'),[i.split('/')[-1] for i in s.get('identifiers',[])])
