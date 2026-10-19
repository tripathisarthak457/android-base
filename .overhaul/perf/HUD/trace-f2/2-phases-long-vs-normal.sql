with longf as (
  select s.id, s.ts, s.dur from slice s join thread_track tt on s.track_id = tt.id
  where tt.utid = 23 and s.name like 'Choreographer#doFrame%' and s.depth = 0 and s.dur > 3e6
), normalf as (
  select s.id, s.ts, s.dur from slice s join thread_track tt on s.track_id = tt.id
  where tt.utid = 23 and s.name like 'Choreographer#doFrame%' and s.depth = 0 and s.dur between 1.0e6 and 2.5e6
)
select 'long' as kind, d.name, count(*) as n, round(avg(d.dur)/1e6,3) as avg_ms, (select count(*) from longf) as frames
from longf f join descendant_slice(f.id) d where d.depth <= 2 group by d.name
union all
select 'normal', d.name, count(*), round(avg(d.dur)/1e6,3), (select count(*) from normalf)
from normalf f join descendant_slice(f.id) d where d.depth <= 2 group by d.name
order by 2, 1;
