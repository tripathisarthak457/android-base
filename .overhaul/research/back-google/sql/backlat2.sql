-- backlat.sql with one change (Oct 19, the phone run): progress writes that repeat the value before them are left out
-- of write_to_present_med, write_to_trav_med, late_draws and writes. The platform hands the card the same progress
-- twice whenever its spring restarts after a pause (every step of bgsteps.sh's slow starts and cancels, a few of the
-- fast swipes'); a repeated value changes no state, so nothing applies or draws it, and backlat.sql counted each one as
-- drawn late (the next apply came frames later). Everything else is identical.
-- Needs the experiment builds' instrumentation (DESIGN.md §5.2, instrument.py): the app counters BackCard.write (raw
-- progress x1000, written in BackCardState.track), BackCard.apply (pose x1000, in applyCard), BackCard.alpha (content
-- alpha x1000, in applyCard) and BackCard.phase (1 started, 2 completed, 3 cancelled, 0 reset). Check the counter track
-- names on a capture with
--   select name, count(*) from process_counter_track t join counter c on c.track_id = t.id group by name
-- Tables, not CTEs: the correlated lookups below would otherwise evaluate a CTE again for every row (minutes a trace).
--
-- One row per gesture (phase 1 to the next phase 0):
--   start_to_shrink_ms   phase -> 1 to the present of the first frame drawn with pose > 0 (the first visible shrink)
--   write_to_present_med median, over the gesture's progress writes, of write -> present of the frame that drew it
--   write_to_trav_med    the same to the start of the main-thread traversal that drew it (same frame: < 8 ms)
--   late_draws           writes whose first traversal comes in a later Choreographer frame than the write
--   release_to_fade_ms   phase -> 2 to the present of the first frame drawn with alpha < 1 (commits only)
--   frames_after_settle  app frames from the release (phase -> 2 or 3) to phase -> 0 (cancels: the tail C1 removes)
create perfetto table bl_app as
  select upid, pid from process where name like 'com.example.wp04.everything%';
create perfetto table bl_mt as
  select utid from thread where upid in (select upid from bl_app) and tid in (select pid from bl_app);
create perfetto table bl_ctr as
  select t.name cname, c.ts, cast(c.value as int) v from counter c join process_counter_track t on c.track_id = t.id
  where t.upid in (select upid from bl_app) and t.name like 'BackCard.%';
create perfetto table bl_fr as
  select sl.ts, sl.dur, cast(substr(sl.name, 23) as int) vsync from slice sl join thread_track tt on sl.track_id = tt.id
  where tt.utid in (select utid from bl_mt) and sl.depth = 0 and sl.name like 'Choreographer#doFrame %';
create perfetto table bl_trav as
  select sl.ts from slice sl join thread_track tt on sl.track_id = tt.id
  where tt.utid in (select utid from bl_mt) and sl.name = 'traversal';
-- each traversal with the doFrame it runs in
create perfetto table bl_tr as
  select t.ts, f.ts fts, f.vsync from bl_trav t join bl_fr f on t.ts >= f.ts and t.ts < f.ts + f.dur;
create perfetto table bl_sf as
  select display_frame_token tok, max(ts + dur) pend from actual_frame_timeline_slice
  where surface_frame_token is null group by display_frame_token;
create perfetto table bl_pres as
  select cast(a.name as int) vsync, sf.pend from actual_frame_timeline_slice a join bl_sf sf on sf.tok = a.display_frame_token
  where a.upid in (select upid from bl_app);
create perfetto table bl_drawn as
  select tr.ts, tr.fts, p.pend from bl_tr tr left join bl_pres p on p.vsync = tr.vsync;
create perfetto table bl_ph as select ts, v from bl_ctr where cname = 'BackCard.phase';
create perfetto table bl_apply as select ts, v from bl_ctr where cname = 'BackCard.apply';
create perfetto table bl_alpha as select ts, v from bl_ctr where cname = 'BackCard.alpha';
create perfetto table bl_gs as
  select p.ts g0, row_number() over (order by p.ts) gesture,
    (select min(q.ts) from bl_ph q where q.v = 0 and q.ts > p.ts) g1
  from bl_ph p where p.v = 1
    -- a card's start writes its first progress in the same callback; a Back key (EDGE_NONE) starts no card
    and exists (select 1 from bl_ctr w where w.cname = 'BackCard.write' and w.ts between p.ts and p.ts + 2e6);
-- the release that ends each gesture: 2 a commit, 3 a cancel (never one from the next gesture)
create perfetto table bl_g as
  select gs.*,
    (select min(q.ts) from bl_ph q where q.v in (2, 3) and q.ts > gs.g0 and q.ts < coalesce(gs.g1, q.ts + 1)) gs2,
    (select q.v from bl_ph q where q.v in (2, 3) and q.ts > gs.g0 and q.ts < coalesce(gs.g1, q.ts + 1) order by q.ts limit 1) kind
  from bl_gs gs;
create perfetto table bl_w as
  select c.ts wts, g.gesture,
    (select min(a.ts) from bl_apply a where a.ts >= c.ts) ats,
    (select max(f.ts) from bl_fr f where f.ts <= c.ts and f.ts + f.dur >= c.ts) wframe
  from (select ts, v, cname, lag(v) over (order by ts) pv from bl_ctr where cname = 'BackCard.write') c
    join bl_g g on c.ts >= g.g0 and c.ts < coalesce(g.g1, c.ts + 1)
  where c.pv is null or c.v != c.pv;
create perfetto table bl_wd as
  select w.*, (select min(d.ts) from bl_drawn d where d.ts >= w.ats) dts from bl_w w;
create perfetto table bl_wp as
  select wd.*, d.pend, d.fts dframe from bl_wd wd left join bl_drawn d on d.ts = wd.dts;
create perfetto table bl_shrink as
  select g.gesture,
    (select min(d.pend) from bl_drawn d where d.ts >= (select min(a.ts) from bl_apply a where a.v > 0 and a.ts >= g.g0)) p
  from bl_g g;
create perfetto table bl_fade as
  select g.gesture,
    (select min(d.pend) from bl_drawn d where d.ts >= (
       select min(a.ts) from bl_alpha a where a.v < 1000 and a.ts >= g.gs2 and a.ts < coalesce(g.g1, a.ts + 1))) p
  from bl_g g;
create perfetto table bl_tail as
  select g.gesture, (select count(*) from bl_fr f where f.ts >= g.gs2 and f.ts < g.g1) n from bl_g g;
create perfetto table bl_med as
  select gesture,
    max(case when rp = (np + 1) / 2 then (pend - wts) end) / 1e6 present_med,
    max(case when rt = (nt + 1) / 2 then (dts - wts) end) / 1e6 trav_med,
    sum(case when dframe is not null and wframe is not null and dframe > wframe then 1 else 0 end) late_draws,
    count(*) writes
  from (select wp.*,
          row_number() over (partition by gesture order by case when pend is null then 1 else 0 end, pend - wts) rp,
          sum(case when pend is null then 0 else 1 end) over (partition by gesture) np,
          row_number() over (partition by gesture order by case when dts is null then 1 else 0 end, dts - wts) rt,
          sum(case when dts is null then 0 else 1 end) over (partition by gesture) nt
        from bl_wp wp)
  group by gesture;
select g.gesture, round(g.g0 / 1e9, 3) uptime_s, case g.kind when 2 then 'commit' when 3 then 'cancel' end kind,
  round((s.p - g.g0) / 1e6, 1) start_to_shrink_ms,
  round(m.present_med, 1) write_to_present_med,
  round(m.trav_med, 1) write_to_trav_med,
  m.late_draws, m.writes,
  round((f.p - g.gs2) / 1e6, 1) release_to_fade_ms,
  t.n frames_after_settle
from bl_g g join bl_shrink s using (gesture) join bl_fade f using (gesture) join bl_tail t using (gesture)
  left join bl_med m using (gesture)
order by g.gesture;
