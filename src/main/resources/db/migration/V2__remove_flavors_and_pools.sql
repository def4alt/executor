drop index if exists executors_flavor_status_idx;

alter table jobs drop column if exists flavor;
alter table executors drop column if exists flavor;

drop table if exists pool_policies;

create index if not exists executors_status_idx on executors (status);
