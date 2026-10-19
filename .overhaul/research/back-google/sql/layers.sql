-- Every HWUI layer render (SkiaPipeline::renderLayerImpl's "drawLayer [name] w x h") on the app's RenderThread, the
-- DrawFrames slice it belongs to (index among all DrawFrames, duration), in ms from the first DrawFrames.
with app as (select p.upid from process p where p.name like 'com.example.wp04.everything%'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
s as (select sl.id, sl.ts, sl.dur, sl.name from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt)),
d as (select ts, dur, row_number() over (order by ts) idx from s where name like 'DrawFrame%'),
t0 as (select min(ts) t0 from d)
select round((s.ts - t0.t0)/1e6, 1) t_ms, round(s.dur/1e6, 2) dur_ms, s.name, d.idx frame, round(d.dur/1e6, 2) frame_ms
from s join d on d.ts <= s.ts and d.ts + d.dur >= s.ts, t0 where s.name like 'drawLayer%' or s.name = 'flush layers' order by s.ts;
