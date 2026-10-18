WITH RECURSIVE per_site AS (
  SELECT callsite_id AS id, SUM(size) AS bytes FROM heap_profile_allocation GROUP BY 1
), up(site, id) AS (
  SELECT id, id FROM per_site
  UNION ALL
  SELECT up.site, c.parent_id FROM up JOIN stack_profile_callsite c ON c.id = up.id WHERE c.parent_id IS NOT NULL
)
SELECT
  CASE WHEN f.name IS NULL OR f.name = '' THEN m.name ELSE substr(f.name, 1, 110) END AS fn,
  SUM(p.bytes) / 1024 AS unreleased_kb
FROM up
JOIN per_site p ON p.id = up.site
JOIN stack_profile_callsite c ON c.id = up.id
JOIN stack_profile_frame f ON f.id = c.frame_id
JOIN stack_profile_mapping m ON m.id = f.mapping
GROUP BY 1 HAVING unreleased_kb > 150 ORDER BY 2 DESC LIMIT 45;
