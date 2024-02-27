create type translator_type as enum ('biocase', 'dwca');

create table source_system
(
    id text not null
        constraint new_source_system_pkey
            primary key,
    name text not null,
    endpoint text not null,
    description text,
    created timestamp with time zone not null,
    deleted timestamp with time zone,
    mapping_id text not null,
    version integer not null,
    creator text not null,
    translator_type translator_type not null
);

create table mapping
(
    id text not null,
    version integer not null,
    name text not null,
    description text,
    mapping jsonb not null,
    created timestamp with time zone not null,
    creator text not null,
    deleted timestamp with time zone,
    sourcedatastandard varchar not null,
    constraint new_mapping_pk
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
    batching_permitted boolean not null
);