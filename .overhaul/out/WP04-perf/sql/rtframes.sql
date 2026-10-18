-- Per RenderThread frame: DrawFrames length, time in text ops while preparing (glyphs rasterised and uploaded) and
-- while executing, other ops, flush, swap; the share of its running time on a big core (CPU 6-7).
with app as (select p.upid from process p join thread t on t.upid = p.upid and t.tid = p.pid where p.name like 'com.example.wp04.everything%' or t.name like '%everything.dev'),
rt as (select utid from thread where upid in (select upid from app) and name = 'RenderThread'),
d as (select sl.id, sl.ts, sl.dur, sl.track_id, tt.utid from slice sl join thread_track tt on sl.track_id = tt.id where tt.utid in (select utid from rt) and sl.name like 'DrawFrames%'),
kids as (select d.id did, c.name, c.dur, (select p.name from slice p where p.id = c.parent_id) parent
         from d join slice c on c.track_id = d.track_id and c.ts >= d.ts and c.ts < d.ts + d.dur and c.id != d.id),
agg as (select did,
  sum(case when name = 'AtlasTextOp' and parent like '%onPrepare%' then dur else 0 end) text_prep,
  sum(case when name = 'AtlasTextOp' and parent like '%onExecute%' then dur else 0 end) text_exec,
  sum(case when name like '%Op' and name != 'AtlasTextOp' and parent like '%onPrepare%' then dur else 0 end) other_prep,
  sum(case when name like '%onPrepare%' then dur else 0 end) prep,
  sum(case when name like '%onExecute%' then dur else 0 end) exec,
  sum(case when name = 'eglSwapBuffersWithDamageKHR' then dur else 0 end) swap,
  sum(case when name = 'syncFrameState' then dur else 0 end) sync,
  sum(case when name like 'Drawing %' then dur else 0 end) drawing,
  sum(case when name = 'flush commands' then dur else 0 end) flush,
  sum(case when name = 'dequeueBuffer' then dur else 0 end) deq,
  sum(case when name = 'AtlasTextOp' then 1 else 0 end) ntext
  from kids group by did),
cpu as (select d.id did, sum(case when ts.cpu >= 6 then min(ts.ts + ts.dur, d.ts + d.dur) - max(ts.ts, d.ts) else 0 end) * 100 /
        sum(min(ts.ts + ts.dur, d.ts + d.dur) - max(ts.ts, d.ts)) big
        from d join thread_state ts on ts.utid = d.utid and ts.state = 'Running' and ts.ts < d.ts + d.dur and ts.ts + ts.dur > d.ts group by d.id)
select round((d.ts - (select min(ts) from d))/1e6) t_ms, round(d.dur/1e6, 2) rt_ms, round(a.sync/1e6, 2) sync, round(a.drawing/1e6, 2) drawing, round(a.flush/1e6, 2) flush, round(a.prep/1e6, 2) prep,
  round(a.text_prep/1e6, 2) text_prep, round(a.other_prep/1e6, 2) oth_prep, round(a.exec/1e6, 2) exec, round(a.text_exec/1e6, 2) text_exec,
  round(a.swap/1e6, 2) swap, round(a.deq/1e6, 2) deq, a.ntext, c.big
from d join agg a on a.did = d.id left join cpu c on c.did = d.id order by d.ts;
