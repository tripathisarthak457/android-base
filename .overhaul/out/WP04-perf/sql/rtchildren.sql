-- Children of the app's RenderThread DrawFrames slices, by name and depth below DrawFrames: count, mean, total.
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
d as (select sl.id, sl.ts, sl.dur, sl.track_id, sl.depth from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%')
select c.depth - d.depth rel, c.name, count(*) n, round(avg(c.dur)/1e6, 3) mean_ms, round(max(c.dur)/1e6, 2) max_ms, round(sum(c.dur)/1e6, 1) total_ms
from slice c join d on c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.id != d.id
where c.depth - d.depth <= 3 group by 1, 2 having total_ms > 5 order by 1, total_ms desc limit 40;
