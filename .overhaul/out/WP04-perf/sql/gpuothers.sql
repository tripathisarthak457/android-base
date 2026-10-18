-- Each app GPU-completion wait over 12 ms (the GPU took that long for one frame), with every other process's
-- GPU-driving threads (GL runners, RenderThreads, kgsl) that ran during it.
with app as (select upid from process where name like 'com.example.wp04%'),
g as (select s.ts, s.dur from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid)
      where t.upid in (select upid from app) and s.name like 'waiting for GPU completion%' and s.dur > 12e6)
select round(g.ts/1e9,3) at_s, round(g.dur/1e6,1) gpu_ms,
  (select group_concat(p.name || ':' || t.name || '=' || round(r/1e6,1), ' ') from (
     select utid, sum(min(sc.ts+sc.dur, g.ts+g.dur) - max(sc.ts, g.ts)) r from sched sc
     where sc.ts < g.ts + g.dur and sc.ts + sc.dur > g.ts group by utid) x join thread t using(utid) join process p using(upid)
   where p.upid not in (select upid from app) and (t.name like '%gl%' or t.name like '%GL%' or t.name = 'RenderThread' or t.name like '%gpu%' or t.name like '%Gpu%')) others
from g order by g.ts;
