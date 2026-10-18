-- CPU callstack samples (linux.perf) of each cold start's main thread in a capture of two alternated apps
-- (startup-ab-perf.win.sh), with the start phase each falls in: pre (fork to bindApplication), bind, act
-- (activityStart), res (activityResume), splash (the resume's end to the first frame with content), frame (that
-- frame), post (to the start's end). One row per sample; the stacks come from startstacks.sql.
-- The main thread is the one that ran the start's bindApplication: without task_rename events a process forked
-- late in the capture keeps the name zygote64 in the process table, so names cannot find it.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select startup_id, ts, ts + dur te, package from android_startups where package like 'com.example.wp04%'),
mainth as (select s.startup_id sid, tt.utid, tt.id track from s join slice sl on sl.name = 'bindApplication' and sl.ts >= s.ts and sl.ts < s.te
           join thread_track tt on tt.id = sl.track_id),
ms as (select sl.name, sl.ts, sl.dur, sl.depth, m.sid from mainth m join s on s.startup_id = m.sid
       join thread_track tt on tt.utid = m.utid join slice sl on sl.track_id = tt.id and sl.ts >= s.ts and sl.ts < s.te),
b as (select sid,
  (select min(ts) from ms m where m.sid = x.sid and name = 'bindApplication') b0,
  (select min(ts) from ms m where m.sid = x.sid and name = 'activityStart') a0,
  (select min(ts) from ms m where m.sid = x.sid and name = 'activityResume') r0,
  (select min(ts + dur) from ms m where m.sid = x.sid and name = 'activityResume') r1,
  (select min(ts) from ms m where m.sid = x.sid and name like 'Choreographer#doFrame%' and depth = 0 and dur > 25e6) f0,
  (select min(ts + dur) from ms m where m.sid = x.sid and name like 'Choreographer#doFrame%' and depth = 0 and dur > 25e6) f1
  from (select distinct sid from ms) x)
select s.package, s.startup_id, ps.ts, ps.callsite_id,
  case when ps.ts < b.b0 then 'pre' when ps.ts < b.a0 then 'bind' when ps.ts < b.r0 then 'act' when ps.ts < b.r1 then 'res'
       when ps.ts < b.f0 then 'splash' when ps.ts < b.f1 then 'frame' else 'post' end phase
from perf_sample ps join mainth m on m.utid = ps.utid join s on s.startup_id = m.sid and ps.ts >= s.ts and ps.ts < s.te
join b on b.sid = s.startup_id order by ps.ts;
