import subprocess, sys, statistics as st
src = sys.argv[1]
pts = [float(l) for l in subprocess.run(['ffprobe','-v','error','-select_streams','v:0','-show_entries','frame=pts_time','-of','csv=p=0',src],capture_output=True,text=True).stdout.split()]
def crop(w,h,x,y):
    raw = subprocess.run(['ffmpeg','-v','error','-i',src,'-fps_mode','passthrough','-vf',f'crop={w}:{h}:{x}:{y}','-f','rawvideo','-pix_fmt','gray','-'],capture_output=True).stdout
    n=len(raw)//(w*h); return [raw[i*w*h:(i+1)*w*h] for i in range(n)], n
ind,n = crop(64,62,78,1330)
print('frames',n,'pts',len(pts),'video span %.3f'%(pts[-1]-pts[0]))
ev=[]
for i in range(1,n):
    a,b=ind[i-1],ind[i]
    d=sum(abs(a[k]-b[k]) for k in range(0,len(a),2))/(len(a)/2)
    if d>3: ev.append(pts[i])
m=[]
for t in ev:
    if not m or t-m[-1]>0.03: m.append(t)
iv=[round(m[i+1]-m[i],3) for i in range(len(m)-1)]
print('indicator steps',len(m),'median %.4f'%st.median(iv) if iv else '', iv[:24])
w=640
blk,n2 = crop(w,20,40,1050)
peaks=[];exc=[]
for f in blk:
    col=[sum(f[r*w+c] for r in range(0,20,4))/5 for c in range(w)]
    mx=max(col); peaks.append(col.index(mx)); exc.append(mx-st.median(col))
wr=[pts[i] for i in range(1,n2) if exc[i]>4 and exc[i-1]>4 and peaks[i-1]-peaks[i]>300]
# also first-appearance-at-left events
print('block wraps',[round(x,3) for x in wr],'periods',[round(wr[i+1]-wr[i],3) for i in range(len(wr)-1)])
for i in range(0,n2,12): print(round(pts[i],3),peaks[i],round(exc[i],1))
