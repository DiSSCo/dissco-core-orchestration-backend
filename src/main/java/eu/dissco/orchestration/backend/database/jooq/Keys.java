/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq;


import eu.dissco.orchestration.backend.database.jooq.tables.DataMapping;
import eu.dissco.orchestration.backend.database.jooq.tables.MachineAnnotationService;
import eu.dissco.orchestration.backend.database.jooq.tables.SourceSystem;
import eu.dissco.orchestration.backend.database.jooq.tables.records.DataMappingRecord;
import eu.dissco.orchestration.backend.database.jooq.tables.records.MachineAnnotationServiceRecord;
import eu.dissco.orchestration.backend.database.jooq.tables.records.SourceSystemRecord;

import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * public.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<DataMappingRecord> DATA_MAPPING_PK = Internal.createUniqueKey(DataMapping.DATA_MAPPING, DSL.name("data_mapping_pk"), new TableField[] { DataMapping.DATA_MAPPING.ID, DataMapping.DATA_MAPPING.VERSION }, true);
    public static final UniqueKey<MachineAnnotationServiceRecord> MACHINE_ANNOTATION_SERVICES_PKEY = Internal.createUniqueKey(MachineAnnotationService.MACHINE_ANNOTATION_SERVICE, DSL.name("machine_annotation_services_pkey"), new TableField[] { MachineAnnotationService.MACHINE_ANNOTATION_SERVICE.ID }, true);
    public static final UniqueKey<SourceSystemRecord> SOURCE_SYSTEM_PKEY = Internal.createUniqueKey(SourceSystem.SOURCE_SYSTEM, DSL.name("source_system_pkey"), new TableField[] { SourceSystem.SOURCE_SYSTEM.ID }, true);
}
