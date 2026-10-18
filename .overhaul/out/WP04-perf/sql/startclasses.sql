-- Class-loading slices (named after a class descriptor, "L...;") and lock contention on the app's main thread during
-- each cold start: count and total ms, per start.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select startup_id, ts, ts + dur te from android_startups where package like '{pkg}%'),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
ms as (select sl.*, s.startup_id sid from slice sl join thread_track tt on sl.track_id = tt.id join s on sl.ts >= s.ts and sl.ts < s.te where tt.utid in (select utid from main))
select sid, 
  sum(case when name like 'L%;' and (select count(*) from ancestor_slice(ms.id) a where a.name like 'L%;') = 0 then 1 else 0 end) classes,
  round(sum(case when name like 'L%;' and (select count(*) from ancestor_slice(ms.id) a where a.name like 'L%;') = 0 then dur else 0 end)/1e6, 1) class_ms,
  sum(case when name like 'Lock contention%' or name like 'monitor contention%' then 1 else 0 end) locks,
  round(sum(case when (name like 'Lock contention%' or name like 'monitor contention%') and depth = (select min(depth) from ms m2 where m2.sid = ms.sid and m2.ts <= ms.ts and m2.ts + m2.dur >= ms.ts + ms.dur and (m2.name like 'Lock contention%' or m2.name like 'monitor contention%')) then dur else 0 end)/1e6, 1) lock_ms
from ms group by sid;
