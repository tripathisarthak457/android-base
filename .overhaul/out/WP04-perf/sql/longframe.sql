-- Every slice inside RenderThread DrawFrames slices over 12 ms (depth below DrawFrames <= 4), with the CPU the
-- RenderThread ran on during the frame and its running / runnable time.
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
d as (select sl.id, sl.ts, sl.dur, sl.track_id, sl.depth, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%' and sl.dur > 12e6)
select d.ts/1e6 frame_ms, round(d.dur/1e6,1) frame_dur, c.depth - d.depth rel, c.name, round(c.dur/1e6, 2) ms,
  (select group_concat(distinct s.cpu) from sched s where s.utid = d.utid and s.ts < d.ts + d.dur and s.ts + s.dur > d.ts) cpus,
  (select round(sum(min(s.ts+s.dur, d.ts+d.dur) - max(s.ts, d.ts))/1e6,1) from sched s where s.utid = d.utid and s.ts < d.ts + d.dur and s.ts + s.dur > d.ts) run_ms
from d join slice c on c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.id != d.id
where c.depth - d.depth <= 4 and c.dur > 0.8e6 order by d.ts, c.ts;
