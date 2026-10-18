
select c.ts, round(c.dur/1e6, 1) ms,
  (select a.name from ancestor_slice(c.id) a where a.name like '%Op' order by a.depth desc limit 1) op,
  (select a.name from ancestor_slice(c.id) a where a.name like 'flush %' limit 1) pass
from slice c where c.name = 'shader_compile'
union all
select sl.ts, round(sl.dur/1e6, 1), 'RenderThread frame', '' from slice sl where sl.name like 'DrawFrames%' and sl.dur > 12e6
order by 1;
