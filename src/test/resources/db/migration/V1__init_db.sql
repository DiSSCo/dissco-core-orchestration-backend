create type translator_type as enum ('biocase', 'dwca');

create table source_system
(
    id text not null
        primary key,
    version integer default 1 not null,
    name text not null,
    endpoint text not null,
    created timestamp with time zone not null,
    modified timestamp with time zone not null,
    tombstoned timestamp with time zone,
    mapping_id text not null,
    creator text not null,
    translator_type translator_type not null,
    data jsonb not null,
    dwc_dp_link text,
    dwca_link text,
    eml bytea
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