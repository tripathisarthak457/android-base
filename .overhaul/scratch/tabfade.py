def bez(x1,y1,x2,y2):
    def f(x):
        lo,hi=0.0,1.0
        for _ in range(60):
            t=(lo+hi)/2
            bx=3*(1-t)**2*t*x1+3*(1-t)*t*t*x2+t**3
            if bx<x: lo=t
            else: hi=t
        t=(lo+hi)/2
        return 3*(1-t)**2*t*y1+3*(1-t)*t*t*y2+t**3
    return f
enter=bez(0.05,0.7,0.1,1); exit_=bez(0.3,0,0.8,0.15)
for name,instant,medium in [("Standard",90,250),("Calm",110,300),("Snappy",60,160),("Bouncy",90,280)]:
    out_d=instant+instant//2
    worst=(0,0)
    for t in range(0,instant+medium+1,5):
        a_out=1-exit_(min(t/out_d,1.0))
        tin=t-instant
        a_in=0 if tin<=0 else enter(min(tin/medium,1.0))
        bg=(1-a_in)*(1-a_out)
        if bg>worst[0]: worst=(bg,t)
    print(f"{name}: background shows through up to {worst[0]*100:.0f}% at t={worst[1]}ms (fade-out {out_d}ms, fade-in delayed {instant}ms over {medium}ms)")
