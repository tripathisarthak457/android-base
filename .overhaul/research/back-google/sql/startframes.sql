-- Needs the experiment builds' BackCard.* counters (DESIGN.md §5.2). For each back gesture (BackCard.phase -> 1, the
-- platform's onBackStarted): the app's first two RenderThread frames after it, each with its DrawFrames length, the
-- layer renders in it (drawLayer: the card's texture rendered), and the GPU completion wait that follows it (the
-- frame's GPU time); and the layer renders in the 400 ms before the gesture (c2's prewarm on the touch-down).
with app as (select upid, pid from process where name like 'com.example.wp04.everything%'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
ath as (select utid from thread where upid in (select upid from app)),
ctr as (select t.name cname, c.ts, c.value v from counter c join process_counter_track t on c.track_id = t.id
        where t.upid in (select upid from app) and t.name like 'BackCard.%'),
-- a card's start writes its first progress in the same callback; a Back key (EDGE_NONE) starts no card
g as (select p.ts g0, row_number() over (order by p.ts) gesture from ctr p where p.cname = 'BackCard.phase' and p.v = 1
      and exists (select 1 from ctr w where w.cname = 'BackCard.write' and w.ts between p.ts and p.ts + 2e6)),
d as (select sl.ts, sl.dur from slice sl join thread_track tt on sl.track_id = tt.id
      where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%'),
lay as (select sl.ts from slice sl join thread_track tt on sl.track_id = tt.id
        where tt.utid in (select utid from rt) and sl.name like 'drawLayer%'),
w as (select sl.ts, sl.dur from slice sl join thread_track tt on sl.track_id = tt.id
      where tt.utid in (select utid from ath) and sl.name like 'waiting for GPU completion%'),
x as (select g.gesture, g.g0, d.ts, d.dur, row_number() over (partition by g.gesture order by d.ts) i
      from g join d on d.ts > g.g0 and d.ts < g.g0 + 100e6)
select x.gesture, round(x.g0 / 1e9, 3) uptime_s, x.i frame, round((x.ts - x.g0) / 1e6, 1) after_start_ms,
  round(x.dur / 1e6, 2) rt_ms,
  (select count(*) from lay where lay.ts between x.ts and x.ts + x.dur) layers,
  (select round(w.dur / 1e6, 2) from w where w.ts >= x.ts order by w.ts limit 1) gpu_ms,
  (select count(*) from lay where lay.ts between x.g0 - 400e6 and x.g0) layers_before
from x where x.i <= 2 order by x.gesture, x.i;
