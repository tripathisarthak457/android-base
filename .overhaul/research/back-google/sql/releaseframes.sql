-- Needs the experiment builds' BackCard.* counters (DESIGN.md §5.2). For each release (BackCard.phase -> 2 commit,
-- 3 cancel): the app's first four frames after it, each with its RenderThread DrawFrames length, whether it rendered
-- a layer (drawLayer: the texture rendered again), FrameTimeline's actual length (vsync to done) and jank type.
-- T renders the texture again on the commit's first frame; C1 should not, and its first frame should already draw
-- the first fade step (check BackCard.alpha < 1000 before that frame's traversal with backlat.sql).
with app as (select upid, pid from process where name like 'com.example.wp04.everything%'),
mt as (select utid from thread where upid in (select upid from app) and tid in (select pid from app)),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
ctr as (select t.name cname, c.ts, cast(c.value as int) v from counter c join process_counter_track t on c.track_id = t.id
        where t.upid in (select upid from app) and t.name like 'BackCard.%'),
-- releases of a card only: a Back key also reaches onBackCompleted (2) but writes no progress before it
rel as (select r.ts, r.v kind, row_number() over (order by r.ts) n from ctr r where r.cname = 'BackCard.phase' and r.v in (2, 3)
        and exists (select 1 from ctr w where w.cname = 'BackCard.write' and w.ts < r.ts and w.ts > r.ts - 3000e6
                    and not exists (select 1 from ctr z where z.cname = 'BackCard.phase' and z.v = 0 and z.ts > w.ts and z.ts < r.ts))),
fr as (select sl.ts, sl.dur, substr(sl.name, 23) vsync from slice sl join thread_track tt on sl.track_id = tt.id
       where tt.utid in (select utid from mt) and sl.depth = 0 and sl.name like 'Choreographer#doFrame %'),
dfr as (select sl.ts, sl.dur, substr(sl.name, 12) vsync from slice sl join thread_track tt on sl.track_id = tt.id
        where tt.utid in (select utid from rt) and sl.name like 'DrawFrames %'),
lay as (select sl.ts from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.name like 'drawLayer%'),
af as (select a.name vsync, a.dur, a.jank_type from actual_frame_timeline_slice a where a.upid in (select upid from app)),
x as (select rel.n, rel.kind, fr.vsync, fr.ts, row_number() over (partition by rel.n order by fr.ts) i
      from rel join fr on fr.ts > rel.ts and fr.ts < rel.ts + 200e6)
select x.n release, round((select ts from rel where rel.n = x.n) / 1e9, 3) uptime_s, case x.kind when 2 then 'commit' else 'cancel' end kind, x.i frame, x.vsync,
  round((x.ts - (select ts from rel where rel.n = x.n)) / 1e6, 1) after_release_ms,
  round(d.dur / 1e6, 2) rt_ms,
  (select count(*) from lay where lay.ts between d.ts and d.ts + d.dur) layer_renders,
  round(af.dur / 1e6, 2) actual_ms, af.jank_type
from x left join dfr d on d.vsync = x.vsync left join af on af.vsync = x.vsync
where x.i <= 4 order by x.n, x.i;
