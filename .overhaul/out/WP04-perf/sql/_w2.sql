select round((s.ts - 1042739815000000)/1e6,2) at_ms, round(s.dur/1e6,2) ms, s.depth, s.name, t.name thread, p.name proc
from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid) left join process p using(upid)
where s.ts < 1042739815000000 + 75e6 and s.ts + s.dur > 1042739815000000 + 20e6 and s.depth <= 2
and coalesce(p.name,'') not like '%surfaceflinger%' and coalesce(p.name,'') not like '%composer%'
and coalesce(p.name,'') not like '%everything%' order by s.ts limit 80;
