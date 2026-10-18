-- Each cold start of {pkg}: the app main thread's time by state over the start (running on big or little cores,
-- runnable, sleeping, IO), and the start's length.
INCLUDE PERFETTO MODULE android.startup.startups;
with s as (select startup_id, ts, ts + dur te, dur from android_startups where package like '{pkg}%'),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
st as (select s.startup_id, ts.state, ts.cpu, min(ts.ts + ts.dur, s.te) - max(ts.ts, s.ts) t
       from thread_state ts join s on ts.ts < s.te and ts.ts + ts.dur > s.ts where ts.utid in (select utid from main))
select s.startup_id, round(s.dur/1e6, 1) start_ms,
  round(sum(case when st.state = 'Running' and st.cpu >= 6 then t else 0 end)/1e6, 1) run_big,
  round(sum(case when st.state = 'Running' and st.cpu < 6 then t else 0 end)/1e6, 1) run_little,
  round(sum(case when st.state in ('R', 'R+') then t else 0 end)/1e6, 1) runnable,
  round(sum(case when st.state = 'S' then t else 0 end)/1e6, 1) sleep,
  round(sum(case when st.state like 'D%' then t else 0 end)/1e6, 1) io
from s join st on st.startup_id = s.startup_id group by s.startup_id;
