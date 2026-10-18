-- The app's main-thread slices nested in Choreographer#doFrame, by name: count, total and mean, over the capture.
with app as (select upid from process where name like 'com.example.wp04.everything%'),
main as (select utid from thread join app using(upid) where thread.tid = (select pid from process where upid = thread.upid)),
frames as (select s.id, s.ts, s.dur from slice s join thread_track tt on s.track_id = tt.id where tt.utid in (select utid from main) and s.name like 'Choreographer#doFrame%' and s.depth = 0)
select d.name, count(*) n, round(sum(d.dur)/1e6, 1) total_ms, round(avg(d.dur)/1e6, 3) mean_ms, round(max(d.dur)/1e6,2) max_ms
from slice d join frames f on d.ts >= f.ts and d.ts < f.ts + f.dur and d.track_id = (select track_id from slice where id = f.id)
where d.depth between 1 and 3
group by d.name order by total_ms desc limit 25;
