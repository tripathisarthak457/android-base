with main as (
  select s.* from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid) join process p using(upid)
  where p.name like 'com.example.wp04.everything%' and t.tid = p.pid
),
keys as (select ts from main where depth = 0 and name like 'receiveMessage%type=KEY%'),
t0 as (select min(ts) as t0 from keys)
select round((m.ts - (select t0 from t0))/1e6, 2) as at_ms, round(m.dur/1e6, 2) as dur_ms, m.depth,
  substr(replace(m.name, 'com.example.wp04.everything.dev/com.example.wp04.everything.MainActivity', 'APP'), 1, 90) as name
from main m
where m.ts >= (select t0 from t0) - 30e6 and m.ts <= (select t0 from t0) + 120e6
  and m.depth <= 2
order by m.ts;
