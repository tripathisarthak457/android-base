#!/usr/bin/env bash
# Summarises a pull-to-refresh Perfetto trace: frame jank types, the slowest frames with what the
# main thread, the render thread and the GPU spent on them, and the GPU frequency while pulling.
#   ptr_trace_report.sh <trace> <package>
set -euo pipefail
T=$1; PKG=$2
TP=$HOME/.local/share/perfetto/prebuilts/trace_processor_shell-55ba613fc6d4f71d
q() { "$TP" -q /dev/stdin "$T" 2>/dev/null <<<"$1"; }

echo "== app frames by jank type"
q "select a.jank_type, count(*) n, round(avg(a.dur)/1e6,2) avg_ms, round(max(a.dur)/1e6,2) max_ms
   from actual_frame_timeline_slice a join process p using(upid)
   where p.name = '$PKG' group by a.jank_type order by n desc;"

echo "== main-thread doFrame and render-thread DrawFrames durations (ms): p50 p90 p99 max"
for name in 'Choreographer#doFrame%' 'DrawFrames%' 'flush commands' 'Record View#draw()' 'traversal' 'Compose:recompose' 'Recomposer:recompose' 'measure' 'layout' 'animation'; do
  q "with d as (select s.dur from slice s join thread_track tt on s.track_id = tt.id join thread t using(utid) join process p using(upid)
       where p.name = '$PKG' and s.name like '$name')
     select '$name', count(*), round((select dur from d order by dur limit 1 offset (select count(*)*50/100 from d))/1e6,2),
       round((select dur from d order by dur limit 1 offset (select count(*)*90/100 from d))/1e6,2),
       round((select dur from d order by dur limit 1 offset (select count(*)*99/100 from d))/1e6,2),
       round(max(dur)/1e6,2) from d;" | tail -n +2
done

echo "== janky app frames: start, duration, type, and the slowest render-thread slices inside"
q "select a.ts, round(a.dur/1e6,2) ms, a.jank_type,
     (select group_concat(n, ' | ') from (select s.name || ' ' || round(s.dur/1e6,1) n from slice s
        join thread_track tt on s.track_id = tt.id join thread t using(utid)
        where t.upid = a.upid and t.name = 'RenderThread' and s.ts between a.ts - 20e6 and a.ts + a.dur and s.depth <= 1
        order by s.dur desc limit 4)) render
   from actual_frame_timeline_slice a join process p using(upid)
   where p.name = '$PKG' and a.jank_type != 'None' order by a.ts limit 40;"

echo "== GPU frequency (MHz) samples while tracing"
q "select round(c.value/1e6) mhz, count(*) n from counter c join gpu_counter_track g on c.track_id = g.id
   where g.name like '%freq%' group by mhz order by mhz;"
q "select t.name, round(c.value) v, count(*) n from counter c join counter_track t on c.track_id = t.id
   where t.name like '%gpufreq%' or t.name like '%gpu_freq%' or t.name like 'GPU%Freq%' group by t.name, v order by t.name, v;"
