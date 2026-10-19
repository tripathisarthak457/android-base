with app as (select p.upid from process p where p.name like 'com.example.wp04.everything%'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread')
select sl.name, count(*) n, round(avg(sl.dur)/1e6,3) avg_ms from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) group by sl.name order by n desc limit 80;
