create table if not exists jobs (
    id varchar(36) primary key,
    script text not null,
    status varchar(32) not null,
    cpus integer not null,
    memory integer not null,
    flavor varchar(64) not null,
    executor_id varchar(36),
    stdout text,
    stderr text,
    exit_code integer,
    created_at timestamp with time zone not null,
    started_at timestamp with time zone,
    finished_at timestamp with time zone
);

create index if not exists jobs_status_created_idx on jobs (status, created_at);

create table if not exists executors (
    id varchar(36) primary key,
    pod_name varchar(255) not null,
    namespace varchar(255) not null,
    flavor varchar(64) not null,
    status varchar(32) not null,
    job_id varchar(36),
    created_at timestamp with time zone not null,
    ready_at timestamp with time zone,
    last_heartbeat_at timestamp with time zone,
    lease_expires_at timestamp with time zone
);

create index if not exists executors_flavor_status_idx on executors (flavor, status);

create table if not exists pool_policies (
    flavor varchar(64) primary key,
    min_ready integer not null,
    max_ready integer not null,
    max_burst_create integer not null,
    idle_ttl_seconds integer not null
);
