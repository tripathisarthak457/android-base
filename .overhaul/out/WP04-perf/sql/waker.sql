-- Sleeps over 10 ms of the wp04 apps' main threads inside activityStart, with the thread that woke each (the
-- sched_wakeup events for the main thread in the 2 ms before the sleep ends): the TracingMuxer artifact of §2e.
with mainth as (select t.utid, t.tid, p.upid, p.name pname from thread t join process p using(upid) where t.tid = p.pid and p.name like 'com.example.wp04%'),
a as (select sl.id, sl.ts, sl.dur, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id where sl.name = 'activityStart' and tt.utid in (select utid from mainth)),
st as (select a.ts ats, ts.ts, ts.dur, ts.utid from thread_state ts join a on ts.utid = a.utid and ts.ts >= a.ts and ts.ts < a.ts + a.dur where ts.state = 'S' and ts.dur > 10e6)
select (select pname from mainth where utid = st.utid) app, round((st.ts - st.ats)/1e6,1) at_ms, round(st.dur/1e6,1) ms,
  (select group_concat(t.name || ':' || e.name, ' ; ') from ftrace_event e left join thread t on t.utid = e.utid
     join args g on g.arg_set_id = e.arg_set_id and g.key = 'pid' and g.int_value = (select tid from mainth where utid = st.utid)
     where e.name in ('sched_wakeup','sched_waking') and e.ts between st.ts + st.dur - 2e6 and st.ts + st.dur + 0.1e6) wakers
from st order by st.ts;
