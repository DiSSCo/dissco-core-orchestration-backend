/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq;


import eu.dissco.orchestration.backend.database.jooq.tables.DataMapping;
import eu.dissco.orchestration.backend.database.jooq.tables.MachineAnnotationService;
import eu.dissco.orchestration.backend.database.jooq.tables.SourceSystem;

import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.data_mapping</code>.
     */
    public final DataMapping DATA_MAPPING = DataMapping.DATA_MAPPING;

    /**
     * The table <code>public.machine_annotation_service</code>.
     */
    public final MachineAnnotationService MACHINE_ANNOTATION_SERVICE = MachineAnnotationService.MACHINE_ANNOTATION_SERVICE;

    /**
     * The table <code>public.source_system</code>.
     */
    public final SourceSystem SOURCE_SYSTEM = SourceSystem.SOURCE_SYSTEM;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            DataMapping.DATA_MAPPING,
            MachineAnnotationService.MACHINE_ANNOTATION_SERVICE,
            SourceSystem.SOURCE_SYSTEM
        );
    }
}
