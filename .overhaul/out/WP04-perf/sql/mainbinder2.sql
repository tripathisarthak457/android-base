-- Synchronous binder calls from {pkg}'s main thread during each cold start, with the slices enclosing them (the
-- call's ancestors on the main thread), and the server thread's own top slice during the call when it has one.
INCLUDE PERFETTO MODULE android.startup.startups;
INCLUDE PERFETTO MODULE android.binder;
with s as (select startup_id, ts, ts + dur te from android_startups where package like '{pkg}%'),
app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}')
select s.startup_id, round((b.client_ts - s.ts)/1e6, 1) at_ms, round(b.client_dur/1e6, 2) dur_ms, replace(b.server_process, '/system/bin/', '') server,
  (select group_concat(substr(a.name, 1, 40), ' > ') from ancestor_slice(b.binder_txn_id) a where a.depth <= 3) inside,
  (select substr(group_concat(r.name, ' | '), 1, 90) from slice r where r.track_id = (select track_id from slice where id = b.binder_reply_id)
     and r.ts >= b.server_ts and r.ts < b.server_ts + b.server_dur and r.depth = 1 + (select depth from slice where id = b.binder_reply_id)) served
from android_binder_txns b join s on b.client_ts >= s.ts and b.client_ts < s.te
where b.client_tid in (select pid from app) and b.is_sync and b.client_dur > 0.4e6 order by s.startup_id, b.client_ts;
