/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq.tables;


import eu.dissco.orchestration.backend.database.jooq.Keys;
import eu.dissco.orchestration.backend.database.jooq.Public;
import eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType;
import eu.dissco.orchestration.backend.database.jooq.tables.records.SourceSystemRecord;

import java.time.Instant;
import java.util.Collection;

import org.jooq.Condition;
import org.jooq.Field;
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
public class SourceSystem extends TableImpl<SourceSystemRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.source_system</code>
     */
    public static final SourceSystem SOURCE_SYSTEM = new SourceSystem();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SourceSystemRecord> getRecordType() {
        return SourceSystemRecord.class;
    }

    /**
     * The column <code>public.source_system.id</code>.
     */
    public final TableField<SourceSystemRecord, String> ID = createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.source_system.name</code>.
     */
    public final TableField<SourceSystemRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.source_system.endpoint</code>.
     */
    public final TableField<SourceSystemRecord, String> ENDPOINT = createField(DSL.name("endpoint"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.source_system.description</code>.
     */
    public final TableField<SourceSystemRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.source_system.created</code>.
     */
    public final TableField<SourceSystemRecord, Instant> CREATED = createField(DSL.name("created"), SQLDataType.INSTANT.nullable(false), this, "");

    /**
     * The column <code>public.source_system.deleted</code>.
     */
    public final TableField<SourceSystemRecord, Instant> DELETED = createField(DSL.name("deleted"), SQLDataType.INSTANT, this, "");

    /**
     * The column <code>public.source_system.mapping_id</code>.
     */
    public final TableField<SourceSystemRecord, String> MAPPING_ID = createField(DSL.name("mapping_id"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.source_system.version</code>.
     */
    public final TableField<SourceSystemRecord, Integer> VERSION = createField(DSL.name("version"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field(DSL.raw("1"), SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>public.source_system.creator</code>.
     */
    public final TableField<SourceSystemRecord, String> CREATOR = createField(DSL.name("creator"), SQLDataType.CLOB.nullable(false).defaultValue(DSL.field(DSL.raw("'0000-0002-5669-2769'::text"), SQLDataType.CLOB)), this, "");

    /**
     * The column <code>public.source_system.translator_type</code>.
     */
    public final TableField<SourceSystemRecord, TranslatorType> TRANSLATOR_TYPE = createField(DSL.name("translator_type"), SQLDataType.VARCHAR.asEnumDataType(TranslatorType.class), this, "");

    private SourceSystem(Name alias, Table<SourceSystemRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private SourceSystem(Name alias, Table<SourceSystemRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.source_system</code> table reference
     */
    public SourceSystem(String alias) {
        this(DSL.name(alias), SOURCE_SYSTEM);
    }

    /**
     * Create an aliased <code>public.source_system</code> table reference
     */
    public SourceSystem(Name alias) {
        this(alias, SOURCE_SYSTEM);
    }

    /**
     * Create a <code>public.source_system</code> table reference
     */
    public SourceSystem() {
        this(DSL.name("source_system"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<SourceSystemRecord> getPrimaryKey() {
        return Keys.NEW_SOURCE_SYSTEM_PKEY;
    }

    @Override
    public SourceSystem as(String alias) {
        return new SourceSystem(DSL.name(alias), this);
    }

    @Override
    public SourceSystem as(Name alias) {
        return new SourceSystem(alias, this);
    }

    @Override
    public SourceSystem as(Table<?> alias) {
        return new SourceSystem(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public SourceSystem rename(String name) {
        return new SourceSystem(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public SourceSystem rename(Name name) {
        return new SourceSystem(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public SourceSystem rename(Table<?> name) {
        return new SourceSystem(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public SourceSystem where(Condition condition) {
        return new SourceSystem(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public SourceSystem where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public SourceSystem where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public SourceSystem where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public SourceSystem where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public SourceSystem where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public SourceSystem where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public SourceSystem where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public SourceSystem whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public SourceSystem whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
