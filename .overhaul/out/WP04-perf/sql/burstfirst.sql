-- The first three RenderThread frames of every burst of frames (a gap of 250 ms or more before it) in the app: their
-- length, and inside them the slices over 0.3 ms below DrawFrames' children (uploads, shader compiles, text ops).
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
d as (select sl.id, sl.ts, sl.dur, sl.track_id, lag(sl.ts) over (order by sl.ts) prev from slice sl join thread_track tt on sl.track_id = tt.id
      where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%'),
b as (select id, ts, dur, track_id, sum(case when prev is null or ts - prev >= 250e6 then 1 else 0 end) over (order by ts) burst from d),
n as (select *, row_number() over (partition by burst order by ts) k from b)
select n.burst, n.k, round(n.dur/1e6, 1) rt_ms,
  (select group_concat(c.name || ' ' || round(c.dur/1e6, 1), ' | ') from slice c where c.track_id = n.track_id and c.ts >= n.ts and c.ts < n.ts + n.dur
     and c.dur > 0.3e6 and c.depth >= 3 and c.name not like 'auto skgpu%' and c.name not in ('queueBuffer', 'flush commands')) inside
from n where n.k <= 3 order by n.burst, n.k;
