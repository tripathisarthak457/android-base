with frames as (
  select s.id, s.ts, s.dur, case when s.dur > 3e6 then 'long' when s.dur between 1.0e6 and 2.5e6 then 'normal' else 'other' end as kind
  from slice s join thread_track tt on s.track_id = tt.id
  where tt.utid = 23 and s.name like 'Choreographer#doFrame%' and s.depth = 0
)
select f.kind, ts.state, ts.cpu, round(sum(min(ts.ts + ts.dur, f.ts + f.dur) - max(ts.ts, f.ts))/1e6, 1) as ms, count(distinct f.id) as frames
from frames f join thread_state ts on ts.utid = 23 and ts.ts < f.ts + f.dur and ts.ts + ts.dur > f.ts
where f.kind != 'other'
group by f.kind, ts.state, ts.cpu order by f.kind, ms desc;
