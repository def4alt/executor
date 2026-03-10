alter table executors drop column if exists last_heartbeat_at;
alter table executors drop column if exists lease_expires_at;
