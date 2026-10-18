INCLUDE PERFETTO MODULE android.startup.startups;
select startup_id, package, startup_type, round(dur/1e6,1) dur_ms from android_startups;
