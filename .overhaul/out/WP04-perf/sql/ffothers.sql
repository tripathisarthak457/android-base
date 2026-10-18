
select round(sum(s.dur)/1e6, 1) ms from sched s join thread t using(utid) where t.name like 'drishti%';
