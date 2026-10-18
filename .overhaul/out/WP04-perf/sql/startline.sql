-- Each cold start of {pkg}: the app main thread's slices (depth <= {depth}, over 1 ms) from the start's beginning to its
-- end (android_startups), as offsets from the start; one row per slice.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select startup_id, ts, ts + dur te from android_startups where package like '{pkg}%'),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid)
select s.startup_id, round((sl.ts - s.ts)/1e6, 1) at_ms, round(sl.dur/1e6, 1) dur_ms, sl.depth, substr(sl.name, 1, 70) name
from slice sl join thread_track tt on sl.track_id = tt.id join s on sl.ts >= s.ts and sl.ts < s.te
where tt.utid in (select utid from main) and sl.depth <= {depth} and sl.dur > 1e6 order by s.startup_id, sl.ts;
