-- Slices inside the app's main-thread frames longer than 15 ms (depth 1..12, over 0.4 ms), in order.
with app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
f as (select sl.id, sl.ts, sl.dur, sl.track_id from slice sl join thread_track tt on sl.track_id = tt.id
      where tt.utid in (select utid from main) and sl.name like 'Choreographer#doFrame%' and sl.depth = 0 and sl.dur > 15e6)
select f.id frame, d.depth, round((d.ts - f.ts)/1e6,1) at_ms, round(d.dur/1e6,2) ms, substr(d.name,1,90) name
from f join slice d on d.track_id = f.track_id and d.ts >= f.ts and d.ts < f.ts + f.dur and d.depth between 1 and 12 and d.dur > 0.4e6
order by f.ts, d.ts;
