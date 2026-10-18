-- startstages.sql for a capture of several wp04 packages, finding each start's main thread as the one that ran its
-- bindApplication (a process forked late in a capture can keep the name zygote64): per start, when bindApplication
-- ran and for how long, activityStart, activityResume and the performResume inside it, the main thread's binder time
-- inside the resume, the first frame with content (the first doFrame over 25 ms after the resume) and its
-- composition and measure, and the start's length.
INCLUDE PERFETTO MODULE android.startup.startups;
INCLUDE PERFETTO MODULE android.binder;
with s as (select startup_id, ts, ts + dur te, dur, package from android_startups where package like 'com.example.wp04%'),
mainth as (select s.startup_id sid, tt.utid from s join slice sl on sl.name = 'bindApplication' and sl.ts >= s.ts and sl.ts < s.te
           join thread_track tt on tt.id = sl.track_id),
ms as (select sl.id, sl.name, sl.ts, sl.dur, sl.depth, sl.track_id, m.sid from mainth m join s on s.startup_id = m.sid
       join thread_track tt on tt.utid = m.utid join slice sl on sl.track_id = tt.id and sl.ts >= s.ts and sl.ts < s.te),
res as (select sid, min(ts) r0, min(ts + dur) r1 from ms where name = 'activityResume' group by sid),
first as (select ms.sid, min(ms.ts) fts from ms join res on res.sid = ms.sid
          where ms.name like 'Choreographer#doFrame%' and ms.depth = 0 and ms.dur > 25e6 and ms.ts > res.r0 group by ms.sid),
fr as (select ms.* from ms join first on ms.sid = first.sid and ms.ts = first.fts and ms.depth = 0 and ms.name like 'Choreographer#doFrame%')
select s.package, s.startup_id,
  round(((select min(ts) from ms where sid = s.startup_id and name = 'bindApplication') - s.ts)/1e6, 1) bind_at,
  round((select max(dur) from ms where sid = s.startup_id and name = 'bindApplication')/1e6, 1) bind,
  round((select max(dur) from ms where sid = s.startup_id and name = 'activityStart')/1e6, 1) act_start,
  round((select max(dur) from ms where sid = s.startup_id and name = 'activityResume')/1e6, 1) act_resume,
  round((select max(dur) from ms where sid = s.startup_id and name like 'performResume:%')/1e6, 1) perf_resume,
  (select count(*) from ms where sid = s.startup_id and name = 'binder transaction'
     and ts >= (select r0 from res where sid = s.startup_id) and ts < (select r1 from res where sid = s.startup_id)) res_binders,
  round((select sum(dur) from ms where sid = s.startup_id and name = 'binder transaction'
     and ts >= (select r0 from res where sid = s.startup_id) and ts < (select r1 from res where sid = s.startup_id))/1e6, 1) res_binder_ms,
  round((fr.ts - s.ts)/1e6, 1) frame_at, round(fr.dur/1e6, 1) frame,
  round((select sum(d.dur) from slice d where d.track_id = fr.track_id and d.ts >= fr.ts and d.ts < fr.ts + fr.dur and d.name = 'animation')/1e6, 1) compose,
  round((select sum(d.dur) from slice d where d.track_id = fr.track_id and d.ts >= fr.ts and d.ts < fr.ts + fr.dur and d.name = 'measure')/1e6, 1) measure,
  round(s.dur/1e6, 1) start_ms
from s left join fr on fr.sid = s.startup_id order by s.startup_id;
