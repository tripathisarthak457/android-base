select count(*) as frames, sum(s.dur > 3e6) as over3ms, sum(s.dur > 2.5e6) as over2_5ms, round(avg(s.dur)/1e6, 3) as mean_ms,
  (select group_concat(ms, ' ') from (select round(s2.dur/1e6,2) as ms from slice s2 join thread_track tt2 on s2.track_id = tt2.id
    where tt2.utid = 23 and s2.name like 'Choreographer#doFrame%' and s2.depth = 0 order by s2.dur desc limit 40)) as top40
from slice s join thread_track tt on s.track_id = tt.id
where tt.utid = 23 and s.name like 'Choreographer#doFrame%' and s.depth = 0;
