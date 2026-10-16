import math
def crit(x0, w, t): return x0*(1+w*t)*math.exp(-w*t)
def under(x0, z, w, t):
    wd = w*math.sqrt(1-z*z)
    return x0*math.exp(-z*w*t)*(math.cos(wd*t) + (z*w/wd)*math.sin(wd*t))
def settle(x0, z, k, thr=1.0):
    w = math.sqrt(k); t=0.0; dt=0.0005; last=0
    while t < 3:
        x = crit(x0,w,t) if z>=1 else under(x0,z,w,t)
        if abs(x) >= thr: last=t
        t+=dt
    return last
W=1080  # px, typical phone width
for name,z,k in [("Standard",1.0,400),("Calm",1.0,300),("Snappy",1.0,700),("Bouncy",0.86,420),("PeriFlow Fluid",1.0,340)]:
    T=settle(W,z,k)
    w=math.sqrt(k)
    row=[]
    for p in (0.1,0.2,0.3,0.5):
        t=p*T
        x = crit(W,w,t) if z>=1 else under(W,z,w,t)
        row.append(f"p={p:.1f}->{(1-x/W)*100:5.1f}%")
    # time to reach 95% and 99%
    def tt(f):
        t=0
        while True:
            x = crit(W,w,t) if z>=1 else under(W,z,w,t)
            if 1-x/W>=f: return t
            t+=0.0005
    print(f"{name:15s} settle(1px)={T*1000:4.0f}ms  95%@{tt(0.95)*1000:4.0f}ms 99%@{tt(0.99)*1000:4.0f}ms  ", "  ".join(row))
# overshoot of sheet springs on modal slide (fraction of height)
for name,z in [("Standard sheet",0.86),("Bouncy sheet",0.68),("Fluid sheet",0.82),("Calm",1.0),("Snappy",0.95)]:
    if z>=1: print(name,"no overshoot"); continue
    os=math.exp(-z*math.pi/math.sqrt(1-z*z))
    print(f"{name:15s} overshoot={os*100:.2f}% of travel -> {os*2400:.0f}px on a 2400px-tall window")
