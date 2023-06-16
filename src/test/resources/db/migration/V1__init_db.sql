CREATE TABLE public.new_source_system (
	id text NOT NULL,
	"name" text NOT NULL,
	endpoint text NOT NULL,
	description text NULL,
	created timestamptz NOT NULL,
	deleted timestamptz NULL,
	mapping_id text NOT NULL,
	CONSTRAINT new_source_system_endpoint_key UNIQUE (endpoint),
	CONSTRAINT new_source_system_pkey PRIMARY KEY (id)
);

CREATE TABLE public.new_mapping (
	id text NOT NULL,
	"version" int4 NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	"mapping" jsonb NOT NULL,
	created timestamptz NOT NULL,
	creator text NOT NULL,
	deleted timestamptz NULL,
	sourcedatastandard varchar NOT NULL,
	CONSTRAINT new_mapping_pk PRIMARY KEY (id, version)
);

create table machine_annotation_services
(
    id text not null primary key,
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
    deleted_on timestamp with time zone
);