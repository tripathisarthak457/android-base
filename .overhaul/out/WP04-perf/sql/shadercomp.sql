-- The app's RenderThread frames over 12 ms, with the shader compiles inside them (count, total ms, cache hits).
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
d as (select sl.id, sl.ts, sl.dur, sl.track_id from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%' and sl.dur > 12e6)
select round((d.ts - (select min(ts) from slice))/1e6) t_ms, round(d.dur/1e6, 1) dur,
  (select count(*) from slice c where c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.name = 'shader_compile') compiles,
  (select round(sum(c.dur)/1e6, 1) from slice c where c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.name = 'shader_compile') compile_ms,
  (select count(*) from slice c where c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.name = 'cache_hit') hits,
  (select group_concat(distinct c.name) from slice c where c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.dur > 3e6 and c.depth >= 3) long_kids
from d order by d.ts;
