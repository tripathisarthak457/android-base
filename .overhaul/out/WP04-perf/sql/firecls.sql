-- Inside each fire-cls slice on the main thread: the thread's states (running, runnable, sleeping, IO) and its
-- child slices summed by name, to tell CPU work from waiting.
with f as (select sl.id, sl.ts, sl.dur, sl.track_id, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id
           where sl.name = 'fire-cls')
select 'state' kind, ts.state || coalesce(':' || ts.io_wait, '') name, count(*) n,
       round(sum(min(ts.ts + ts.dur, f.ts + f.dur) - max(ts.ts, f.ts)) / 1e6 / 3.0, 2) per_start_ms
from thread_state ts join f on ts.utid = f.utid and ts.ts < f.ts + f.dur and ts.ts + ts.dur > f.ts
group by 2
union all
select 'child', d.name, count(*), round(sum(d.dur)/1e6/3.0, 2)
from slice d join f on d.track_id = f.track_id and d.ts > f.ts and d.ts < f.ts + f.dur and d.depth = 2
group by d.name
order by kind, per_start_ms desc;
