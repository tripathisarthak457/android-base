-- When Google Photos' GL runner and MediaProvider ran in a capture: per 250 ms bucket, CPU ms of each.
select cast(s.ts/250e6 as int)*0.25 bucket_s,
  round(sum(case when t.name like 'drishti%' then s.dur else 0 end)/1e6,1) photos_gl_ms,
  round(sum(case when p.name like '%providers.media%' then s.dur else 0 end)/1e6,1) mediaprov_ms,
  round(sum(case when p.name = 'com.google.android.apps.photos' then s.dur else 0 end)/1e6,1) photos_ms
from sched s join thread t using(utid) left join process p using(upid)
where (t.name like 'drishti%' or p.name like '%providers.media%' or p.name = 'com.google.android.apps.photos')
group by 1 having photos_ms + mediaprov_ms > 1 order by 1;
