/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq.tables;


import eu.dissco.orchestration.backend.database.jooq.Keys;
import eu.dissco.orchestration.backend.database.jooq.Public;
import eu.dissco.orchestration.backend.database.jooq.tables.records.NewSourceSystemRecord;

import java.time.Instant;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function7;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row7;
import org.jooq.Schema;
import org.jooq.SelectField;
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
public class NewSourceSystem extends TableImpl<NewSourceSystemRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.new_source_system</code>
     */
    public static final NewSourceSystem NEW_SOURCE_SYSTEM = new NewSourceSystem();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<NewSourceSystemRecord> getRecordType() {
        return NewSourceSystemRecord.class;
    }

    /**
     * The column <code>public.new_source_system.id</code>.
     */
    public final TableField<NewSourceSystemRecord, String> ID = createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.new_source_system.name</code>.
     */
    public final TableField<NewSourceSystemRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.new_source_system.endpoint</code>.
     */
    public final TableField<NewSourceSystemRecord, String> ENDPOINT = createField(DSL.name("endpoint"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.new_source_system.description</code>.
     */
    public final TableField<NewSourceSystemRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.new_source_system.created</code>.
     */
    public final TableField<NewSourceSystemRecord, Instant> CREATED = createField(DSL.name("created"), SQLDataType.INSTANT.nullable(false), this, "");

    /**
     * The column <code>public.new_source_system.deleted</code>.
     */
    public final TableField<NewSourceSystemRecord, Instant> DELETED = createField(DSL.name("deleted"), SQLDataType.INSTANT, this, "");

    /**
     * The column <code>public.new_source_system.mapping_id</code>.
     */
    public final TableField<NewSourceSystemRecord, String> MAPPING_ID = createField(DSL.name("mapping_id"), SQLDataType.CLOB.nullable(false), this, "");

    private NewSourceSystem(Name alias, Table<NewSourceSystemRecord> aliased) {
        this(alias, aliased, null);
    }

    private NewSourceSystem(Name alias, Table<NewSourceSystemRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.new_source_system</code> table reference
     */
    public NewSourceSystem(String alias) {
        this(DSL.name(alias), NEW_SOURCE_SYSTEM);
    }

    /**
     * Create an aliased <code>public.new_source_system</code> table reference
     */
    public NewSourceSystem(Name alias) {
        this(alias, NEW_SOURCE_SYSTEM);
    }

    /**
     * Create a <code>public.new_source_system</code> table reference
     */
    public NewSourceSystem() {
        this(DSL.name("new_source_system"), null);
    }

    public <O extends Record> NewSourceSystem(Table<O> child, ForeignKey<O, NewSourceSystemRecord> key) {
        super(child, key, NEW_SOURCE_SYSTEM);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<NewSourceSystemRecord> getPrimaryKey() {
        return Keys.NEW_SOURCE_SYSTEM_PKEY;
    }

    @Override
    public NewSourceSystem as(String alias) {
        return new NewSourceSystem(DSL.name(alias), this);
    }

    @Override
    public NewSourceSystem as(Name alias) {
        return new NewSourceSystem(alias, this);
    }

    @Override
    public NewSourceSystem as(Table<?> alias) {
        return new NewSourceSystem(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public NewSourceSystem rename(String name) {
        return new NewSourceSystem(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public NewSourceSystem rename(Name name) {
        return new NewSourceSystem(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public NewSourceSystem rename(Table<?> name) {
        return new NewSourceSystem(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row7 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row7<String, String, String, String, Instant, Instant, String> fieldsRow() {
        return (Row7) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function7<? super String, ? super String, ? super String, ? super String, ? super Instant, ? super Instant, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function7<? super String, ? super String, ? super String, ? super String, ? super Instant, ? super Instant, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
