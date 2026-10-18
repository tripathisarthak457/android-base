-- Every slice longer than 0.5 ms, on any thread of any process, overlapping [1042739815000000, 1042739880000000] (ns), with thread and process.
select round((s.ts - 1042739815000000)/1e6,2) at_ms, round(s.dur/1e6,2) ms, s.depth, s.name, t.name thread, p.name proc
from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid) left join process p using(upid)
where s.ts < 1042739880000000 and s.ts + s.dur > 1042739815000000 and s.dur > 0.5e6 and s.depth <= 3 order by s.ts limit 120;
