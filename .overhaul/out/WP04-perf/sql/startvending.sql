-- Per cold start of the wp04 apps: the processes started during it, and the CPU the Play Store and GMS processes ran
-- inside it (Play's update check binds a service of com.android.vending; §2e of the WP04-perf handoff).
INCLUDE PERFETTO MODULE android.startup.startups;
select s.startup_id, s.package,
  (select group_concat(distinct p.name) from process p where p.start_ts between s.ts and s.ts + s.dur) procs_started,
  (select round(sum(ts.dur)/1e6,1) from thread_state ts join thread t using(utid) join process p using(upid) where p.name like 'com.android.vending%' and ts.state = 'Running' and ts.ts between s.ts and s.ts + s.dur) vending_run_ms,
  (select round(sum(ts.dur)/1e6,1) from thread_state ts join thread t using(utid) join process p using(upid) where p.name like 'com.google.android.gms%' and ts.state = 'Running' and ts.ts between s.ts and s.ts + s.dur) gms_run_ms
from android_startups s where s.package like 'com.example%' order by s.startup_id;
