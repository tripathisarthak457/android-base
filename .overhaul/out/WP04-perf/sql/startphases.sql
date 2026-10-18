-- Each cold start of the wp04 apps in a capture: the main thread's running and waiting time per start phase (before
-- bindApplication, bindApplication, activityStart, activityResume, the resume's end to the first frame with content, that
-- frame onward). Matches main threads by process name, so a start whose process kept the name zygote64 is left out.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select startup_id, ts, ts + dur te, dur, package from android_startups where package like 'com.example.wp04%'),
mainth as (select t.utid, p.name pname from thread t join process p using(upid) where t.tid = p.pid and p.name like 'com.example.wp04%'),
ms as (select sl.name, sl.ts, sl.dur, sl.depth, s.startup_id sid from slice sl join thread_track tt on sl.track_id = tt.id join s on sl.ts >= s.ts and sl.ts < s.te join mainth m on m.utid = tt.utid and s.package = m.pname),
b as (select sid,
  (select min(ts) from ms m where m.sid = x.sid and name = 'bindApplication') b0,
  (select min(ts) from ms m where m.sid = x.sid and name = 'activityStart') a0,
  (select min(ts) from ms m where m.sid = x.sid and name = 'activityResume') r0,
  (select min(ts + dur) from ms m where m.sid = x.sid and name = 'activityResume') r1,
  (select min(ts) from ms m where m.sid = x.sid and name like 'Choreographer#doFrame%' and depth = 0 and dur > 25e6) f0
  from (select distinct sid from ms) x),
st as (select s.startup_id sid, s.package, ts.state, ts.ts, ts.dur from thread_state ts join s on ts.ts >= s.ts and ts.ts < s.te join mainth m on m.utid = ts.utid and m.pname = s.package)
select s.package, s.startup_id, round(s.dur/1e6,1) total,
 round(sum(case when st.ts < b.b0 and st.state = 'Running' then st.dur end)/1e6,1) pre_run,
 round(sum(case when st.ts >= b.b0 and st.ts < b.a0 and st.state = 'Running' then st.dur end)/1e6,1) bind_run,
 round(sum(case when st.ts >= b.b0 and st.ts < b.a0 and st.state != 'Running' then st.dur end)/1e6,1) bind_wait,
 round(sum(case when st.ts >= b.a0 and st.ts < b.r0 and st.state = 'Running' then st.dur end)/1e6,1) act_run,
 round(sum(case when st.ts >= b.a0 and st.ts < b.r0 and st.state != 'Running' then st.dur end)/1e6,1) act_wait,
 round(sum(case when st.ts >= b.r0 and st.ts < b.r1 and st.state = 'Running' then st.dur end)/1e6,1) res_run,
 round(sum(case when st.ts >= b.r0 and st.ts < b.r1 and st.state != 'Running' then st.dur end)/1e6,1) res_wait,
 round(sum(case when st.ts >= b.r1 and st.ts < b.f0 and st.state = 'Running' then st.dur end)/1e6,1) splash_run,
 round(sum(case when st.ts >= b.r1 and st.ts < b.f0 and st.state != 'Running' then st.dur end)/1e6,1) splash_wait,
 round(sum(case when st.ts >= b.f0 and st.state = 'Running' then st.dur end)/1e6,1) frame_run,
 round(sum(case when st.ts >= b.f0 and st.state != 'Running' then st.dur end)/1e6,1) frame_wait
from s join b on b.sid = s.startup_id join st on st.sid = s.startup_id group by s.startup_id order by s.package, s.startup_id;
