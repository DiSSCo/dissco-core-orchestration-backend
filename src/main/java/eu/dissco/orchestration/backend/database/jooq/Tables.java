/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq;


import eu.dissco.orchestration.backend.database.jooq.tables.DataMapping;
import eu.dissco.orchestration.backend.database.jooq.tables.MachineAnnotationServices;
import eu.dissco.orchestration.backend.database.jooq.tables.Mapping;
import eu.dissco.orchestration.backend.database.jooq.tables.NewMachineAnnotationServices;
import eu.dissco.orchestration.backend.database.jooq.tables.NewSourceSystem;
import eu.dissco.orchestration.backend.database.jooq.tables.SourceSystem;


/**
 * Convenience access to all tables in public.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>public.data_mapping</code>.
     */
    public static final DataMapping DATA_MAPPING = DataMapping.DATA_MAPPING;

    /**
     * The table <code>public.machine_annotation_services</code>.
     */
    public static final MachineAnnotationServices MACHINE_ANNOTATION_SERVICES = MachineAnnotationServices.MACHINE_ANNOTATION_SERVICES;

    /**
     * The table <code>public.mapping</code>.
     */
    public static final Mapping MAPPING = Mapping.MAPPING;

    /**
     * The table <code>public.new_machine_annotation_services</code>.
     */
    public static final NewMachineAnnotationServices NEW_MACHINE_ANNOTATION_SERVICES = NewMachineAnnotationServices.NEW_MACHINE_ANNOTATION_SERVICES;

    /**
     * The table <code>public.new_source_system</code>.
     */
    public static final NewSourceSystem NEW_SOURCE_SYSTEM = NewSourceSystem.NEW_SOURCE_SYSTEM;

    /**
     * The table <code>public.source_system</code>.
     */
    public static final SourceSystem SOURCE_SYSTEM = SourceSystem.SOURCE_SYSTEM;
}
