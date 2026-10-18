-- What the main thread sleeps on inside fire-cls: each S/D interval, its length, and the thread that woke it.
with f as (select sl.id, sl.ts, sl.dur, sl.track_id, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id
           where sl.name = 'fire-cls')
select f.id fid, round((ts.ts - f.ts)/1e6, 2) at_ms, ts.state, round(ts.dur/1e6, 2) dur_ms, ts.blocked_function,
       wt.name waker, wt.tid
from thread_state ts join f on ts.utid = f.utid and ts.ts >= f.ts and ts.ts < f.ts + f.dur
left join thread wt on wt.utid = ts.waker_utid
where ts.state in ('S', 'D') and ts.dur > 0.3e6 order by f.id, ts.ts;
