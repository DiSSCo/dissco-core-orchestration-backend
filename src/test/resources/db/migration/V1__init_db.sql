create type translator_type as enum ('biocase', 'dwca');

create table new_source_system
(
    id text not null
        constraint newnew_source_system_pkey
            primary key,
    name text not null,
    endpoint text not null,
    date_created timestamp with time zone not null,
    date_modified timestamp with time zone not null,
    date_tombstoned timestamp with time zone,
    mapping_id text not null,
    version integer default 1 not null,
    creator text not null,
    translator_type translator_type,
    data jsonb not null
);

create table data_mapping
(
    id                   text                     not null,
    version              integer                  not null,
    name                 text                     not null,
    date_created         timestamp with time zone not null,
    date_modified        timestamp with time zone not null,
    date_tombstoned      timestamp with time zone,
    creator              text                     not null,
    mapping_data_standard varchar                  not null,
    data                 jsonb                    not null,
    constraint data_mapping_pk
        primary key (id, version)
);

create table machine_annotation_services
(
    id text not null
        primary key,
    version integer not null,
    name varchar not null,
    created timestamp with time zone not null,
    administrator text not null,
    container_image text not null,
    container_image_tag text not null,
    target_digital_object_filters jsonb,
    service_description text,
    service_state text,
    source_code_repository text,
    service_availability text,
    code_maintainer text,
    code_license text,
    dependencies text[],
    support_contact text,
    sla_documentation text,
    topicname text,
    maxreplicas integer,
    deleted_on timestamp with time zone,
    batching_permitted boolean not null,
    time_to_live integer not null
);

create table new_machine_annotation_services
(
    id                     text                     not null
        primary key,
    version                integer                  not null,
    name                   varchar                  not null,
    date_created           timestamp with time zone not null,
    date_modified          timestamp with time zone not null,
    date_tombstoned        timestamp with time zone,
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
    data                   jsonb                    not null
);