
with app as (select upid from process where name like 'com.example.wp04%')
select a.ts, a.dur, a.jank_type, a.present_type from actual_frame_timeline_slice a
where a.upid in (select upid from app) order by a.ts;
