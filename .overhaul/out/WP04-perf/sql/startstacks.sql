-- Every callsite of the capture's callstack samples with its frame and mapping, for startprofile.py.
select c.id, coalesce(c.parent_id, -1) parent, f.name frame, m.name mapping
from stack_profile_callsite c join stack_profile_frame f on f.id = c.frame_id join stack_profile_mapping m on m.id = f.mapping;
