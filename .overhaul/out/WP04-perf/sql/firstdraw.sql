-- Descendants of each start's first traversal on the app's main thread (depth relative to it), mean over the starts.
with app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%everything.dev'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
tr as (select sl.id, sl.ts, sl.dur, sl.track_id, sl.depth from slice sl join thread_track tt on sl.track_id = tt.id
       where tt.utid in (select utid from main) and sl.name = 'traversal' and sl.dur > 30e6)
select d.depth - tr.depth rel, d.name, count(*) n, round(sum(d.dur)/1e6/(select count(*) from tr), 2) per_start_ms
from slice d join tr on d.track_id = tr.track_id and d.ts >= tr.ts and d.ts < tr.ts + tr.dur and d.id != tr.id
where d.dur > 0.4e6 and d.depth - tr.depth <= 6 group by 1, 2 order by 1, per_start_ms desc limit 80;
