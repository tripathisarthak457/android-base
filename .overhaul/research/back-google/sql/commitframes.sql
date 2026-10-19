-- App frames around each layer render: FrameTimeline's actual frame (vsync-to-present length, jank) and the
-- RenderThread's DrawFrames length, in ms from the first DrawFrames.
with app as (select p.upid from process p where p.name like 'com.example.wp04.everything%'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
s as (select sl.ts, sl.dur, sl.name from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt)),
d as (select ts, dur, row_number() over (order by ts) idx from s where name like 'DrawFrame%'),
t0 as (select min(ts) t0 from d),
lay as (select d.idx from s join d on d.ts <= s.ts and d.ts + d.dur >= s.ts where s.name like 'drawLayer%'),
win as (select d.idx, d.ts, d.dur from d, lay where d.idx between lay.idx - 3 and lay.idx + 4)
select distinct win.idx frame, round((win.ts - t0.t0)/1e6, 1) t_ms, round(win.dur/1e6, 2) rt_ms,
  case when win.idx in (select idx from lay) then 'LAYER' else '' end layer,
  (select round(a.dur/1e6, 2) from actual_frame_timeline_slice a where a.upid in (select upid from app) and a.ts <= win.ts and a.ts + a.dur >= win.ts order by a.ts desc limit 1) actual_ms,
  (select a.jank_type from actual_frame_timeline_slice a where a.upid in (select upid from app) and a.ts <= win.ts and a.ts + a.dur >= win.ts order by a.ts desc limit 1) jank
from win, t0 order by win.idx;
