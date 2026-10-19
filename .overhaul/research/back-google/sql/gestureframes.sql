-- Each main-thread Choreographer#doFrame from a gesture's first layer render to its commit-time one: which phases ran
-- (animation = where the platform's back-progress spring ticks; Recomposer:animation = Compose's frame clock;
-- traversal = a draw), in ms from the first DrawFrames, and the gap to the next RenderThread frame.
with app as (select p.upid, p.pid from process p where p.name like 'com.example.wp04.everything%'),
mt as (select utid from thread where upid in (select upid from app) and tid in (select pid from app)),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
s as (select sl.ts, sl.dur, sl.name from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt)),
t0 as (select min(ts) t0 from s where name like 'DrawFrame%'),
lay as (select ts, row_number() over (order by ts) n from s where name like 'drawLayer%'),
g as (select a.ts g0, b.ts g1, (a.n + 1) / 2 gesture from lay a join lay b on b.n = a.n + 1 where a.n % 2 = 1),
m as (select sl.id, sl.ts, sl.dur, sl.name, sl.depth, sl.parent_id from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from mt)),
f as (select m.id, m.ts, m.dur, g.gesture from m, g where m.name like 'Choreographer#doFrame%' and m.depth = 0 and m.ts between g.g0 - 12e6 and g.g1)
select f.gesture, round((f.ts - t0.t0)/1e6, 1) t_ms, round(f.dur/1e6, 2) dur_ms,
  max(case when c.name = 'animation' then 1 else 0 end) anim,
  max(case when c.name = 'Recomposer:animation' then 1 else 0 end) recomposer_anim,
  max(case when c.name = 'Compose:recompose' then 1 else 0 end) recompose,
  max(case when c.name = 'traversal' then 1 else 0 end) trav
from f join m c on c.ts >= f.ts and c.ts < f.ts + f.dur and c.depth between 1 and 3, t0
group by f.id order by f.ts;
