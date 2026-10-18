SELECT SUM(size) / 1024 AS unreleased_kb, SUM(CASE WHEN size > 0 THEN size ELSE 0 END) / 1024 AS allocated_kb FROM heap_profile_allocation;
