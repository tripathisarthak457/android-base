-- Every HWUI layer render on the app's RenderThread with its trace time in seconds, the same clock as the
-- /proc/uptime stamps in back-trace.win.sh's steps.txt (CLOCK_BOOTTIME), so each render can be put in its step:
-- one per back gesture in c1 and c2 (two in tex, none in head), one per edge touch in c2 only, none while an edge
-- scroll moves the detail. warm = c2's BackCard.warm counter at that time (null in the other builds).
with app as (select upid from process where name like 'com.example.wp04.everything%'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
w as (select c.ts, c.value from counter c join process_counter_track t on c.track_id = t.id where t.upid in (select upid from app) and t.name = 'BackCard.warm')
select round(sl.ts / 1e9, 3) uptime_s, round(sl.dur / 1e6, 2) dur_ms, sl.name,
  (select cast(w.value as int) from w where w.ts <= sl.ts order by w.ts desc limit 1) warm
from slice sl join thread_track tt on sl.track_id = tt.id
where tt.utid in (select utid from rt) and sl.name like 'drawLayer%' order by sl.ts;
