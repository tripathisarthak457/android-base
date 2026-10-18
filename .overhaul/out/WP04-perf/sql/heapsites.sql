select c.id, c.parent_id, f.name fn, m.name lib from stack_profile_callsite c
join stack_profile_frame f on c.frame_id = f.id join stack_profile_mapping m on f.mapping = m.id