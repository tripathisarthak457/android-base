import math
def x_at(x0,z,w,t):
    if z>=1: return x0*(1+w*t)*math.exp(-w*t)
    wd=w*math.sqrt(1-z*z)
    return x0*math.exp(-z*w*t)*(math.cos(wd*t)+(z*w/wd)*math.sin(wd*t))
def settle(x0,z,k,thr):
    w=math.sqrt(k);t=0;last=0;dt=0.0002
    while t<3:
        if abs(x_at(x0,z,w,t))>=thr: last=t
        t+=dt
    return last
W=1080
for name,z,k in [("Standard",1.0,400),("Bouncy",0.86,420),("Calm",1.0,300),("Snappy",1.0,700)]:
    T=settle(W,z,k,0.01); w=math.sqrt(k)
    row=[f"p={p}:{(1-x_at(W,z,w,p*T)/W)*100:.1f}%" for p in (0.1,0.2,0.3,0.5)]
    print(f"{name:9s} duration(thr 0.01px)={T*1000:.0f}ms  (thr 1px={settle(W,z,k,1.0)*1000:.0f}ms) ", " ".join(row))
