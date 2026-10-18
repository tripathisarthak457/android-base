-- RenderThread DrawFrames over 7 ms: how the thread spent them (running on a little or a big core, runnable but not
-- running, sleeping, in IO) and its deepest long child slice.
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
d as (select sl.id, sl.ts, sl.dur, sl.track_id, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%' and sl.dur > 7e6),
st as (select d.id, ts.state, ts.cpu, min(ts.ts + ts.dur, d.ts + d.dur) - max(ts.ts, d.ts) t from d join thread_state ts on ts.utid = d.utid and ts.ts < d.ts + d.dur and ts.ts + ts.dur > d.ts),
agg as (select id, round(sum(case when state = 'Running' and cpu < 6 then t else 0 end)/1e6, 2) run_little,
  round(sum(case when state = 'Running' and cpu >= 6 then t else 0 end)/1e6, 2) run_big,
  round(sum(case when state like 'R%' then t else 0 end)/1e6, 2) runnable,
  round(sum(case when state = 'S' then t else 0 end)/1e6, 2) sleep,
  round(sum(case when state like 'D%' then t else 0 end)/1e6, 2) io from st group by id),
kid as (select d.id, c.name, c.dur, row_number() over (partition by d.id order by c.depth desc, c.dur desc) rn
        from d join slice c on c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.dur > 0.6 * d.dur and c.id != d.id)
select round((d.ts - (select min(ts) from slice))/1e6) t_ms, round(d.dur/1e6, 2) dur, a.run_little, a.run_big, a.runnable, a.sleep, a.io, k.name deepest
from d join agg a on a.id = d.id left join kid k on k.id = d.id and k.rn = 1 order by d.ts;
