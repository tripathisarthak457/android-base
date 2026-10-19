-- Back gestures (each starts where the app gets the MotionEvent CANCEL of the system taking the pointer): for the
-- app's next 40 frames, vsync-to-present latency (FrameTimeline: the app frame's expected start to the end of the
-- SurfaceFlinger display frame that presented it), in ms, '|' after a frame followed by a skipped vsync. The first
-- frame of a transition from idle stuffs the buffer queue, and each skipped vsync drains one buffer (29 ms floor on
-- this phone at 120 Hz). avg_1_10 and avg_11_30 are the means of frames 1-10 and 11-30.
with app as (select upid, pid from process where name like 'com.example.wp04.everything%'),
mt as (select utid from thread where upid in (select upid from app) and tid in (select pid from app)),
g as (select sl.ts, row_number() over (order by sl.ts) gesture from slice sl join thread_track tt on sl.track_id = tt.id
      where tt.utid in (select utid from mt) and sl.depth = 0 and sl.name like 'dispatchInputEvent MotionEvent CANCEL%'),
af as (select a.name, a.ts, a.display_frame_token tok from actual_frame_timeline_slice a where a.upid in (select upid from app)),
sf as (select s.display_frame_token tok, max(s.ts + s.dur) pend from actual_frame_timeline_slice s
       where s.surface_frame_token is null and s.display_frame_token in (select tok from af) group by s.display_frame_token),
ex as (select e.name, e.ts from expected_frame_timeline_slice e where e.upid in (select upid from app)),
f as (select ex.ts ets, (sf.pend - ex.ts) / 1e6 lat from af join ex on ex.name = af.name left join sf on sf.tok = af.tok),
gf as (select g.gesture, g.ts g0, f.ets, f.lat,
         row_number() over (partition by g.gesture order by f.ets) i,
         lead(f.ets) over (partition by g.gesture order by f.ets) nets
       from g join f on f.ets > g.ts and f.ets < g.ts + 1500e6),
w as (select * from gf where i <= 40)
select gesture, round(g0 / 1e9, 3) uptime_s, round((min(ets) - g0) / 1e6, 1) cancel_to_first_vsync_ms,
  round(avg(case when i between 1 and 10 then lat end), 1) avg_1_10,
  round(avg(case when i between 11 and 30 then lat end), 1) avg_11_30,
  sum(case when i <= 30 and nets - ets > 12.5e6 then 1 else 0 end) skips_1_30,
  group_concat(cast(round(lat) as int) || case when nets - ets > 12.5e6 then '|' else '' end, ' ') lat_ms
from w group by gesture order by gesture;
