-- The app's heaviest main-thread frames (doFrame over 4 ms): duration, CPU it mostly ran on, and its biggest children.
with app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
f as (select sl.id, sl.ts, sl.dur, sl.track_id, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id
      where tt.utid in (select utid from main) and sl.name like 'Choreographer#doFrame%' and sl.depth = 0 and sl.dur > 4e6)
select round((f.ts - (select min(ts) from f))/1e9, 2) at_s, round(f.dur/1e6,1) ms,
  (select round(sum(case when ts.cpu >= 6 then min(ts.ts+ts.dur, f.ts+f.dur) - max(ts.ts, f.ts) else 0 end)*100.0/f.dur) from thread_state ts where ts.utid = f.utid and ts.state = 'Running' and ts.ts < f.ts+f.dur and ts.ts+ts.dur > f.ts) big_pct,
  (select round(sum(case when ts.state = 'Running' then min(ts.ts+ts.dur, f.ts+f.dur) - max(ts.ts, f.ts) else 0 end)*100.0/f.dur) from thread_state ts where ts.utid = f.utid and ts.ts < f.ts+f.dur and ts.ts+ts.dur > f.ts) run_pct,
  (select group_concat(name || ' ' || round(dur/1e6,1), ', ') from (select d.name, d.dur from slice d where d.track_id = f.track_id and d.ts >= f.ts and d.ts < f.ts + f.dur and d.depth between 1 and 4 and d.dur > 0.7e6 order by d.dur desc limit 7)) top
from f order by f.ts;
