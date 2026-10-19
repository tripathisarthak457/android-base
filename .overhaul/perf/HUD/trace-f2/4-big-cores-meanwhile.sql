with longf as (
  select s.id, s.ts, s.dur from slice s join thread_track tt on s.track_id = tt.id
  where tt.utid = 23 and s.name like 'Choreographer#doFrame%' and s.depth = 0 and s.dur > 3e6
), littleRun as (
  select max(ts.ts, f.ts) as a, min(ts.ts + ts.dur, f.ts + f.dur) as b from longf f join thread_state ts
  on ts.utid = 23 and ts.state = 'Running' and ts.cpu < 6 and ts.ts < f.ts + f.dur and ts.ts + ts.dur > f.ts
)
select t.name as thread, p.name as process, sc.cpu, round(sum(min(sc.ts + sc.dur, l.b) - max(sc.ts, l.a))/1e6, 2) as ms
from littleRun l join sched sc on sc.cpu in (6, 7) and sc.ts < l.b and sc.ts + sc.dur > l.a
join thread t using(utid) left join process p using(upid)
group by t.name, p.name, sc.cpu order by ms desc limit 25;
