-- Inside each cold start's first Record View#draw() (over 8 ms) on the app's main thread: descendants by name and
-- depth below it, mean per start; plus the thread's states during it.
with app as (select p.upid, p.pid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like '{pkg}%' or t.name like '%{short}'),
main as (select t.utid from thread t join app a on t.upid = a.upid where t.tid = a.pid),
r as (select sl.id, sl.ts, sl.dur, sl.track_id, sl.depth, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id
       where tt.utid in (select utid from main) and sl.name = 'Record View#draw()' and sl.dur > 8e6)
select 'slice' kind, d.depth - r.depth rel, d.name, count(*) n, round(sum(d.dur)/1e6/(select count(*) from r), 2) per_start_ms
from slice d join r on d.track_id = r.track_id and d.ts >= r.ts and d.ts < r.ts + r.dur and d.id != r.id
where d.dur > 0.2e6 group by 2, 3
union all
select 'state', 0, ts.state || ' cpu' || (case when ts.cpu >= 6 then 'big' else 'little' end), count(*),
  round(sum(min(ts.ts + ts.dur, r.ts + r.dur) - max(ts.ts, r.ts))/1e6/(select count(*) from r), 2)
from thread_state ts join r on ts.utid = r.utid and ts.ts < r.ts + r.dur and ts.ts + ts.dur > r.ts group by 3
union all
select 'count', 0, 'records', count(*), round(avg(dur)/1e6, 2) from r
order by kind, rel, per_start_ms desc;
