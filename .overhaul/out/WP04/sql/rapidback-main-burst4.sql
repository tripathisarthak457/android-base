with main as (
  select s.* from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid) join process p using(upid)
  where p.name like 'com.example.wp04.everything%' and t.tid = p.pid
),
keys as (select ts from main where depth = 0 and name like 'receiveMessage%type=KEY%'),
starts as (select ts, row_number() over (order by ts) as n from (select ts, lag(ts) over (order by ts) as prev from keys) where prev is null or ts - prev > 1e9),
t0 as (select ts as t0 from starts where n = 4)
select round((m.ts - (select t0 from t0))/1e6, 2) as at_ms, round(m.dur/1e6, 2) as dur_ms, m.depth, substr(m.name, 1, 60) as name
from main m
where m.ts >= (select t0 from t0) - 5e6 and m.ts <= (select t0 from t0) + 40e6 and m.depth <= 2
  and m.name not like 'preDispatch%' and m.name not like 'NativePre%' and m.name not like 'sendMessage%'
order by m.ts;
