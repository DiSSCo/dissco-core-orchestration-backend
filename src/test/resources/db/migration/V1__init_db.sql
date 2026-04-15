create type translator_type as enum ('biocase', 'dwca');

create table source_system
(
    id              text                     not null
        primary key,
    version         integer default 1        not null,
    name            text                     not null
        constraint name_unique
            unique,
    endpoint        text                     not null,
    created         timestamp with time zone not null,
    modified        timestamp with time zone not null,
    tombstoned      timestamp with time zone,
    mapping_id      text                     not null,
    creator         text                     not null,
    translator_type translator_type          not null,
    data            jsonb                    not null,
    dwc_dp_link     text,
    dwca_link       text,
    eml             bytea,
    filters         text[],
    constraint endpoint_unique
        unique nulls not distinct (endpoint, tombstoned, filters)
);

create table data_mapping
(
    id                   text                     not null,
    version              integer                  not null,
    name                 text                     not null,
    created         timestamp with time zone not null,
    modified        timestamp with time zone not null,
    tombstoned      timestamp with time zone,
    creator              text                     not null,
    mapping_data_standard varchar                  not null,
    data                 jsonb                    not null,
    constraint data_mapping_pk
        primary key (id, version)
);

create table machine_annotation_service
(
    id                     text                     not null
        primary key,
    version                integer                  not null,
    name                   varchar                  not null,
    created           timestamp with time zone not null,
    modified          timestamp with time zone not null,
    tombstoned        timestamp with time zone,
    creator                text                     not null,
    container_image        text                     not null,
    container_image_tag    text                     not null,
    creative_work_state    text,
    source_code_repository text,
    service_availability   text,
    code_maintainer        text,
    code_license           text,
    batching_permitted     boolean,
    time_to_live           integer default 86400    not null,
    data                   jsonb                    not null,
    environment            jsonb,
    secrets                jsonb
);

create type error_code as enum ('TIMEOUT', 'DISSCO_EXCEPTION', 'MAS_EXCEPTION');

create type job_state as enum ('SCHEDULED', 'RUNNING', 'FAILED', 'COMPLETED', 'QUEUED', 'NOTIFICATION_FAILED');

create table translator_job_record
(
    job_id            uuid                     not null,
    job_state         job_state                not null,
    source_system_id  text                     not null,
    time_started      timestamp with time zone not null,
    time_completed    timestamp with time zone,
    processed_records int,
    report            jsonb,
    error             error_code,
    PRIMARY KEY (job_id, source_system_id),
    FOREIGN KEY (source_system_id) REFERENCES source_system (id)
);