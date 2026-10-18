SELECT m.name AS mapping, f.name AS frame,
       SUM(a.size) / 1024 AS unreleased_kb, SUM(CASE WHEN a.size > 0 THEN a.size ELSE 0 END) / 1024 AS allocated_kb
FROM heap_profile_allocation a
JOIN stack_profile_callsite c ON a.callsite_id = c.id
JOIN stack_profile_frame f ON c.frame_id = f.id
JOIN stack_profile_mapping m ON f.mapping = m.id
GROUP BY 1, 2 ORDER BY 3 DESC LIMIT 12;
