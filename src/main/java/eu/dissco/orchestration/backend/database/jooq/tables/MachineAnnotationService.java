/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq.tables;


import eu.dissco.orchestration.backend.database.jooq.Keys;
import eu.dissco.orchestration.backend.database.jooq.Public;
import eu.dissco.orchestration.backend.database.jooq.tables.records.MachineAnnotationServiceRecord;

import java.time.Instant;
import java.util.Collection;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MachineAnnotationService extends TableImpl<MachineAnnotationServiceRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.machine_annotation_service</code>
     */
    public static final MachineAnnotationService MACHINE_ANNOTATION_SERVICE = new MachineAnnotationService();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MachineAnnotationServiceRecord> getRecordType() {
        return MachineAnnotationServiceRecord.class;
    }

    /**
     * The column <code>public.machine_annotation_service.id</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> ID = createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.machine_annotation_service.version</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, Integer> VERSION = createField(DSL.name("version"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.machine_annotation_service.name</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>public.machine_annotation_service.date_created</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, Instant> DATE_CREATED = createField(DSL.name("date_created"), SQLDataType.INSTANT.nullable(false), this, "");

    /**
     * The column <code>public.machine_annotation_service.date_modified</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, Instant> DATE_MODIFIED = createField(DSL.name("date_modified"), SQLDataType.INSTANT.nullable(false), this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.date_tombstoned</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, Instant> DATE_TOMBSTONED = createField(DSL.name("date_tombstoned"), SQLDataType.INSTANT, this, "");

    /**
     * The column <code>public.machine_annotation_service.creator</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> CREATOR = createField(DSL.name("creator"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.container_image</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> CONTAINER_IMAGE = createField(DSL.name("container_image"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.container_image_tag</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> CONTAINER_IMAGE_TAG = createField(DSL.name("container_image_tag"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.creative_work_state</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> CREATIVE_WORK_STATE = createField(DSL.name("creative_work_state"), SQLDataType.CLOB, this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.source_code_repository</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> SOURCE_CODE_REPOSITORY = createField(DSL.name("source_code_repository"), SQLDataType.CLOB, this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.service_availability</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> SERVICE_AVAILABILITY = createField(DSL.name("service_availability"), SQLDataType.CLOB, this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.code_maintainer</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> CODE_MAINTAINER = createField(DSL.name("code_maintainer"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.machine_annotation_service.code_license</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> CODE_LICENSE = createField(DSL.name("code_license"), SQLDataType.CLOB, this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.support_contact</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, String> SUPPORT_CONTACT = createField(DSL.name("support_contact"), SQLDataType.CLOB, this, "");

    /**
     * The column
     * <code>public.machine_annotation_service.batching_permitted</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, Boolean> BATCHING_PERMITTED = createField(DSL.name("batching_permitted"), SQLDataType.BOOLEAN, this, "");

    /**
     * The column <code>public.machine_annotation_service.time_to_live</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, Integer> TIME_TO_LIVE = createField(DSL.name("time_to_live"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field(DSL.raw("86400"), SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>public.machine_annotation_service.data</code>.
     */
    public final TableField<MachineAnnotationServiceRecord, JSONB> DATA = createField(DSL.name("data"), SQLDataType.JSONB.nullable(false), this, "");

    /**
     * The column <code>public.machine_annotation_service.environment</code>.
     * Environmental variables to supply to the MAS, non-sensitive
     */
    public final TableField<MachineAnnotationServiceRecord, JSONB> ENVIRONMENT = createField(DSL.name("environment"), SQLDataType.JSONB, this, "Environmental variables to supply to the MAS, non-sensitive");

    /**
     * The column <code>public.machine_annotation_service.secrets</code>. Remote
     * secrets to supply to the MAS
     */
    public final TableField<MachineAnnotationServiceRecord, JSONB> SECRETS = createField(DSL.name("secrets"), SQLDataType.JSONB, this, "Remote secrets to supply to the MAS");

    private MachineAnnotationService(Name alias, Table<MachineAnnotationServiceRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private MachineAnnotationService(Name alias, Table<MachineAnnotationServiceRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.machine_annotation_service</code> table
     * reference
     */
    public MachineAnnotationService(String alias) {
        this(DSL.name(alias), MACHINE_ANNOTATION_SERVICE);
    }

    /**
     * Create an aliased <code>public.machine_annotation_service</code> table
     * reference
     */
    public MachineAnnotationService(Name alias) {
        this(alias, MACHINE_ANNOTATION_SERVICE);
    }

    /**
     * Create a <code>public.machine_annotation_service</code> table reference
     */
    public MachineAnnotationService() {
        this(DSL.name("machine_annotation_service"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<MachineAnnotationServiceRecord> getPrimaryKey() {
        return Keys.MACHINE_ANNOTATION_SERVICES_PKEY;
    }

    @Override
    public MachineAnnotationService as(String alias) {
        return new MachineAnnotationService(DSL.name(alias), this);
    }

    @Override
    public MachineAnnotationService as(Name alias) {
        return new MachineAnnotationService(alias, this);
    }

    @Override
    public MachineAnnotationService as(Table<?> alias) {
        return new MachineAnnotationService(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public MachineAnnotationService rename(String name) {
        return new MachineAnnotationService(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MachineAnnotationService rename(Name name) {
        return new MachineAnnotationService(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public MachineAnnotationService rename(Table<?> name) {
        return new MachineAnnotationService(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public MachineAnnotationService where(Condition condition) {
        return new MachineAnnotationService(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public MachineAnnotationService where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public MachineAnnotationService where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public MachineAnnotationService where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public MachineAnnotationService where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public MachineAnnotationService where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public MachineAnnotationService where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public MachineAnnotationService where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public MachineAnnotationService whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public MachineAnnotationService whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
