/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq.tables;


import eu.dissco.orchestration.backend.database.jooq.Keys;
import eu.dissco.orchestration.backend.database.jooq.Public;
import eu.dissco.orchestration.backend.database.jooq.tables.records.MappingRecord;

import java.time.Instant;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function9;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row9;
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
public class Mapping extends TableImpl<MappingRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.mapping</code>
     */
    public static final Mapping MAPPING = new Mapping();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MappingRecord> getRecordType() {
        return MappingRecord.class;
    }

    /**
     * The column <code>public.mapping.id</code>.
     */
    public final TableField<MappingRecord, String> ID = createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.mapping.version</code>.
     */
    public final TableField<MappingRecord, Integer> VERSION = createField(DSL.name("version"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.mapping.name</code>.
     */
    public final TableField<MappingRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.mapping.description</code>.
     */
    public final TableField<MappingRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.mapping.mapping</code>.
     */
    public final TableField<MappingRecord, JSONB> MAPPING_ = createField(DSL.name("mapping"), SQLDataType.JSONB.nullable(false), this, "");

    /**
     * The column <code>public.mapping.created</code>.
     */
    public final TableField<MappingRecord, Instant> CREATED = createField(DSL.name("created"), SQLDataType.INSTANT.nullable(false), this, "");

    /**
     * The column <code>public.mapping.creator</code>.
     */
    public final TableField<MappingRecord, String> CREATOR = createField(DSL.name("creator"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.mapping.deleted</code>.
     */
    public final TableField<MappingRecord, Instant> DELETED = createField(DSL.name("deleted"), SQLDataType.INSTANT, this, "");

    /**
     * The column <code>public.mapping.sourcedatastandard</code>.
     */
    public final TableField<MappingRecord, String> SOURCEDATASTANDARD = createField(DSL.name("sourcedatastandard"), SQLDataType.VARCHAR.nullable(false), this, "");

    private Mapping(Name alias, Table<MappingRecord> aliased) {
        this(alias, aliased, null);
    }

    private Mapping(Name alias, Table<MappingRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.mapping</code> table reference
     */
    public Mapping(String alias) {
        this(DSL.name(alias), MAPPING);
    }

    /**
     * Create an aliased <code>public.mapping</code> table reference
     */
    public Mapping(Name alias) {
        this(alias, MAPPING);
    }

    /**
     * Create a <code>public.mapping</code> table reference
     */
    public Mapping() {
        this(DSL.name("mapping"), null);
    }

    public <O extends Record> Mapping(Table<O> child, ForeignKey<O, MappingRecord> key) {
        super(child, key, MAPPING);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<MappingRecord> getPrimaryKey() {
        return Keys.NEW_MAPPING_PK;
    }

    @Override
    public Mapping as(String alias) {
        return new Mapping(DSL.name(alias), this);
    }

    @Override
    public Mapping as(Name alias) {
        return new Mapping(alias, this);
    }

    @Override
    public Mapping as(Table<?> alias) {
        return new Mapping(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Mapping rename(String name) {
        return new Mapping(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Mapping rename(Name name) {
        return new Mapping(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Mapping rename(Table<?> name) {
        return new Mapping(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<String, Integer, String, String, JSONB, Instant, String, Instant, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function9<? super String, ? super Integer, ? super String, ? super String, ? super JSONB, ? super Instant, ? super String, ? super Instant, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function9<? super String, ? super Integer, ? super String, ? super String, ? super JSONB, ? super Instant, ? super String, ? super Instant, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
