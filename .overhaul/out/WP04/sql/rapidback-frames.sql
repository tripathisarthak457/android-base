with main as (
  select s.* from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid) join process p using(upid)
  where p.name like 'com.example.wp04.everything%' and t.tid = p.pid
),
keys as (select ts from main where depth = 0 and name like 'receiveMessage%type=KEY%'),
-- a burst starts with a key 1 s or more after the previous one
starts as (select ts from (select ts, lag(ts) over (order by ts) as prev from keys) where prev is null or ts - prev > 1e9)
select (select count(*) from starts s2 where s2.ts <= a.ts) as burst,
  round((a.ts - (select max(s.ts) from starts s where s.ts <= a.ts))/1e6, 1) as at_ms,
  round(a.dur/1e6, 1) as dur_ms, a.name as vsync, a.jank_type, a.present_type, a.on_time_finish
from actual_frame_timeline_slice a join process p using(upid)
where p.name like 'com.example.wp04.everything%'
  and a.ts >= (select min(ts) from starts) - 10e6
  and exists (select 1 from starts s where a.ts >= s.ts - 10e6 and a.ts <= s.ts + 400e6)
order by a.ts;
