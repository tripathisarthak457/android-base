-- Each cold start of {pkg}, as offsets from the start (android_startups): when bindApplication, activityStart and
-- activityResume ran and for how long, the first frame that draws content (the first doFrame over 25 ms after the
-- resume) with its composition, measure, layout and recording, the RenderThread's frame for it, and the start's end.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select startup_id, ts, ts + dur te, dur from android_startups where package like '{pkg}%'),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
rt as (select t.utid from thread t join app a on t.upid = a.upid where t.name = 'RenderThread'),
ms as (select sl.*, s.startup_id sid, s.ts sts from slice sl join thread_track tt on sl.track_id = tt.id join s on sl.ts >= s.ts and sl.ts < s.te
       where tt.utid in (select utid from main)),
first as (select sid, min(ts) fts from ms where name like 'Choreographer#doFrame%' and depth = 0 and dur > 25e6
          and ts > (select max(m2.ts) from ms m2 where m2.sid = ms.sid and m2.name = 'activityResume') group by sid),
fr as (select ms.* from ms join first on ms.sid = first.sid and ms.ts = first.fts and ms.depth = 0 and ms.name like 'Choreographer#doFrame%'),
inside as (select fr.sid, d.name, sum(d.dur) dur from fr join slice d on d.track_id = fr.track_id and d.ts >= fr.ts and d.ts < fr.ts + fr.dur
           where d.name in ('animation', 'measure', 'layout', 'Record View#draw()', 'postAndWait') group by fr.sid, d.name),
rtf as (select fr.sid, min(sl.ts) rts from fr join slice sl join thread_track tt on sl.track_id = tt.id
        where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%' and sl.ts >= fr.ts and sl.ts < fr.ts + fr.dur group by fr.sid)
select s.startup_id,
  round(((select min(ts) from ms where sid = s.startup_id and name = 'bindApplication') - s.ts)/1e6, 1) bind_at,
  round((select max(dur) from ms where sid = s.startup_id and name = 'bindApplication')/1e6, 1) bind,
  round((select max(dur) from ms where sid = s.startup_id and name = 'activityStart')/1e6, 1) act_start,
  round((select max(dur) from ms where sid = s.startup_id and name = 'activityResume')/1e6, 1) act_resume,
  round((fr.ts - s.ts)/1e6, 1) frame_at, round(fr.dur/1e6, 1) frame,
  round((select dur from inside where sid = s.startup_id and name = 'animation')/1e6, 1) compose,
  round((select dur from inside where sid = s.startup_id and name = 'measure')/1e6, 1) measure,
  round((select dur from inside where sid = s.startup_id and name = 'layout')/1e6, 1) layout,
  round((select dur from inside where sid = s.startup_id and name = 'Record View#draw()')/1e6, 1) record,
  round((select sl.dur from rtf join slice sl on sl.ts = rtf.rts and sl.name like 'DrawFrames%' where rtf.sid = s.startup_id)/1e6, 1) rt_frame,
  round(s.dur/1e6, 1) start_ms
from s left join fr on fr.sid = s.startup_id order by s.startup_id;
