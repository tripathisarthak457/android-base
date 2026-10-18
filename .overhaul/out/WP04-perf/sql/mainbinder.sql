-- Synchronous binder calls from {pkg}'s main thread during each cold start: when, how long, to which process and
-- which AIDL method (where the trace names it).
INCLUDE PERFETTO MODULE android.startup.startups;
INCLUDE PERFETTO MODULE android.binder;
with s as (select startup_id, ts, ts + dur te from android_startups where package like '{pkg}%'),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}')
select s.startup_id, round((b.client_ts - s.ts)/1e6, 1) at_ms, round(b.client_dur/1e6, 2) dur_ms, b.server_process, b.aidl_name
from android_binder_txns b join s on b.client_ts >= s.ts and b.client_ts < s.te
where b.client_tid in (select pid from app) and b.is_sync and b.client_dur > 0.4e6 order by s.startup_id, b.client_ts;
