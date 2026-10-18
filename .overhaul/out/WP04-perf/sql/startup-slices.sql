-- Main-thread slices of the app's process during each cold start, by name: mean per start and total over the three.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select startup_id, ts, ts + dur as te from android_startups where package like 'com.example.wp04.everything%'),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid)
select sl.name, sl.depth, count(*) n, round(sum(sl.dur)/1e6/3.0, 2) per_start_ms, round(max(sl.dur)/1e6, 2) max_ms
from slice sl join thread_track tt on sl.track_id = tt.id join s on sl.ts >= s.ts and sl.ts < s.te
where tt.utid in (select utid from main) and sl.depth <= 2 and sl.dur > 1e6
group by sl.name, sl.depth order by per_start_ms desc limit 40;
