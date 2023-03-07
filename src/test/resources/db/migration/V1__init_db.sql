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