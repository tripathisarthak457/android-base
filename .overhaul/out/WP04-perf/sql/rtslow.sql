-- The app's RenderThread DrawFrames slices: by duration bucket, the share of their running time on the big cores
-- (6, 7) and the mean frequency of the CPU they ran on.
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
d as (select sl.id, sl.ts, sl.dur, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%'),
run as (select d.id, d.dur, ts.cpu, min(ts.ts + ts.dur, d.ts + d.dur) - max(ts.ts, d.ts) as t, ts.ts as sts
        from d join thread_state ts on ts.utid = d.utid and ts.state = 'Running' and ts.ts < d.ts + d.dur and ts.ts + ts.dur > d.ts),
freq as (select r.id, r.t, r.cpu, (select c.value from counter c join cpu_counter_track ct on c.track_id = ct.id
          where ct.name = 'cpufreq' and ct.cpu = r.cpu and c.ts <= r.sts order by c.ts desc limit 1) as khz, r.dur from run r)
select case when dur < 3e6 then 'a <3 ms' when dur < 5e6 then 'b 3-5 ms' when dur < 8e6 then 'c 5-8 ms' else 'd >=8 ms' end bucket,
  count(distinct id) frames, round(sum(case when cpu >= 6 then t else 0 end) * 100.0 / sum(t)) big_pct,
  round(sum(t * coalesce(khz, 0)) / sum(t) / 1000) mean_mhz
from freq group by bucket order by bucket;
