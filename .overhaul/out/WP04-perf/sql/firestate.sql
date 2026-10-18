-- Main-thread states inside each Firebase component init (fire-*), mean ms per start, with the CPU it ran on.
with app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
f as (select sl.name, sl.ts, sl.dur, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from main) and (sl.name like 'fire-%' or sl.name = 'bindApplication'))
select f.name, ts.state, coalesce(ts.cpu, -1) >= 6 as big, round(sum(min(ts.ts + ts.dur, f.ts + f.dur) - max(ts.ts, f.ts))/1e6/3.0, 2) per_start_ms
from f join thread_state ts on ts.utid = f.utid and ts.ts < f.ts + f.dur and ts.ts + ts.dur > f.ts
group by f.name, ts.state, big order by f.name, per_start_ms desc;
