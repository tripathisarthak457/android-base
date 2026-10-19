-- Main-thread slices (depth <= 4) in the two UI frames before each commit-time layer render.
with app as (select p.upid, p.pid from process p where p.name like 'com.example.wp04.everything%'),
mt as (select utid from thread where upid in (select upid from app) and tid in (select pid from app)),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
s as (select sl.ts, sl.dur, sl.name from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt)),
d as (select ts from s where name like 'DrawFrame%'),
t0 as (select min(ts) t0 from d),
lay as (select ts from s where name like 'drawLayer%')
select round((m.ts - t0.t0)/1e6, 2) t_ms, round(m.dur/1e6, 3) dur_ms, m.depth, substr(m.name, 1, 90) name
from slice m join thread_track tt on m.track_id = tt.id, t0
where tt.utid in (select utid from mt) and m.depth <= 4
  and exists (select 1 from lay where m.ts between lay.ts - 20e6 and lay.ts)
  and (m.name not like 'Choreographer#doFrame%' or m.depth = 0)
order by m.ts;
