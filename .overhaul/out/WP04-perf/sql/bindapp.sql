-- Descendants of bindApplication on the app's main thread, per start: name, depth, mean ms over the three starts.
with app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
b as (select sl.id, sl.ts, sl.dur, sl.track_id from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from main) and sl.name = 'bindApplication')
select d.name, d.depth, count(*) n, round(sum(d.dur)/1e6/3.0, 2) per_start_ms
from slice d join b on d.track_id = b.track_id and d.ts >= b.ts and d.ts < b.ts + b.dur and d.id != b.id
where d.dur > 0.5e6 group by d.name, d.depth order by d.depth, per_start_ms desc limit 60;
