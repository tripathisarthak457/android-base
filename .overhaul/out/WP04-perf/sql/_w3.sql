-- CPU usage by process in the window
select p.name proc, t.name thread, round(sum(min(s.ts+s.dur, 1042739815000000+75e6) - max(s.ts, 1042739815000000+20e6))/1e6,1) run_ms, group_concat(distinct s.cpu) cpus
from sched s join thread t using(utid) left join process p using(upid)
where s.ts < 1042739815000000 + 75e6 and s.ts + s.dur > 1042739815000000 + 20e6 and t.tid != 0
group by 1,2 order by 3 desc limit 25;
