select a.upid, p.name proc, p.pid, a.callsite_id, sum(a.size) bytes from heap_profile_allocation a
join process p using(upid) group by 1, 4 having bytes > 0