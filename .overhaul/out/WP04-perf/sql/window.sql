-- For start {sid} of {pkg}: every slice (over 0.3 ms) of the app's threads between {a} and {b} ms after the start,
-- and the main thread's states there.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select ts from android_startups where startup_id = {sid}),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}'),
th as (select t.utid, t.tid, t.name, t.tid = a.pid is_main from thread t join app a on t.upid = a.upid)
select 'slice' k, th.name thread, round((sl.ts - s.ts)/1e6, 1) at_ms, round(sl.dur/1e6, 2) dur_ms, sl.depth, substr(sl.name, 1, 80) name
from slice sl join thread_track tt on sl.track_id = tt.id join th on tt.utid = th.utid, s
where sl.ts >= s.ts + {a}*1e6 and sl.ts < s.ts + {b}*1e6 and sl.dur > 0.3e6 and sl.depth <= 4
union all
select 'state', 'main', round((ts.ts - s.ts)/1e6, 1), round(ts.dur/1e6, 2), ts.cpu, ts.state || coalesce(' ' || ts.blocked_function, '') || coalesce(' woken by ' || (select name from thread where utid = ts.waker_utid), '')
from thread_state ts join th on ts.utid = th.utid and th.is_main, s
where ts.ts + ts.dur > s.ts + {a}*1e6 and ts.ts < s.ts + {b}*1e6 and ts.dur > 0.3e6
order by k, thread, at_ms;
