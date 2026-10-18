-- The app's RenderThread slices by name (depth 0..2): count, mean and total, over the capture.
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread')
select case when sl.name like 'DrawFrames%' then 'DrawFrames' else sl.name end name, sl.depth, count(*) n,
  round(avg(sl.dur)/1e6, 3) mean_ms, round(sum(sl.dur)/1e6, 1) total_ms
from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.depth <= 2
group by 1, sl.depth having total_ms > 20 order by total_ms desc limit 14;
