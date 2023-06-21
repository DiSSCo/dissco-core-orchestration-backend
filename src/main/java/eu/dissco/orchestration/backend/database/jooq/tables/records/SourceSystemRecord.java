/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq.tables.records;


import eu.dissco.orchestration.backend.database.jooq.tables.SourceSystem;
import java.time.Instant;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class SourceSystemRecord extends UpdatableRecordImpl<SourceSystemRecord> implements
    Record9<String, String, String, String, Instant, Instant, String, Integer, String> {

  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.source_system.id</code>.
   */
  public void setId(String value) {
    set(0, value);
  }

  /**
   * Getter for <code>public.source_system.id</code>.
   */
  public String getId() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.source_system.name</code>.
   */
  public void setName(String value) {
    set(1, value);
  }

  /**
   * Getter for <code>public.source_system.name</code>.
   */
  public String getName() {
    return (String) get(1);
  }

  /**
   * Setter for <code>public.source_system.endpoint</code>.
   */
  public void setEndpoint(String value) {
    set(2, value);
  }

  /**
   * Getter for <code>public.source_system.endpoint</code>.
   */
  public String getEndpoint() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.source_system.description</code>.
   */
  public void setDescription(String value) {
    set(3, value);
  }

  /**
   * Getter for <code>public.source_system.description</code>.
   */
  public String getDescription() {
    return (String) get(3);
  }

  /**
   * Setter for <code>public.source_system.created</code>.
   */
  public void setCreated(Instant value) {
    set(4, value);
  }

  /**
   * Getter for <code>public.source_system.created</code>.
   */
  public Instant getCreated() {
    return (Instant) get(4);
  }

  /**
   * Setter for <code>public.source_system.deleted</code>.
   */
  public void setDeleted(Instant value) {
    set(5, value);
  }

  /**
   * Getter for <code>public.source_system.deleted</code>.
   */
  public Instant getDeleted() {
    return (Instant) get(5);
  }

  /**
   * Setter for <code>public.source_system.mapping_id</code>.
   */
  public void setMappingId(String value) {
    set(6, value);
  }

  /**
   * Getter for <code>public.source_system.mapping_id</code>.
   */
  public String getMappingId() {
    return (String) get(6);
  }

  /**
   * Setter for <code>public.source_system.version</code>.
   */
  public void setVersion(Integer value) {
    set(7, value);
  }

  /**
   * Getter for <code>public.source_system.version</code>.
   */
  public Integer getVersion() {
    return (Integer) get(7);
  }

  /**
   * Setter for <code>public.source_system.creator</code>.
   */
  public void setCreator(String value) {
    set(8, value);
  }

  /**
   * Getter for <code>public.source_system.creator</code>.
   */
  public String getCreator() {
    return (String) get(8);
  }

  // -------------------------------------------------------------------------
  // Primary key information
  // -------------------------------------------------------------------------

  @Override
  public Record1<String> key() {
    return (Record1) super.key();
  }

  // -------------------------------------------------------------------------
  // Record9 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row9<String, String, String, String, Instant, Instant, String, Integer, String> fieldsRow() {
    return (Row9) super.fieldsRow();
  }

  @Override
  public Row9<String, String, String, String, Instant, Instant, String, Integer, String> valuesRow() {
    return (Row9) super.valuesRow();
  }

  @Override
  public Field<String> field1() {
    return SourceSystem.SOURCE_SYSTEM.ID;
  }

  @Override
  public Field<String> field2() {
    return SourceSystem.SOURCE_SYSTEM.NAME;
  }

  @Override
  public Field<String> field3() {
    return SourceSystem.SOURCE_SYSTEM.ENDPOINT;
  }

  @Override
  public Field<String> field4() {
    return SourceSystem.SOURCE_SYSTEM.DESCRIPTION;
  }

  @Override
  public Field<Instant> field5() {
    return SourceSystem.SOURCE_SYSTEM.CREATED;
  }

  @Override
  public Field<Instant> field6() {
    return SourceSystem.SOURCE_SYSTEM.DELETED;
  }

  @Override
  public Field<String> field7() {
    return SourceSystem.SOURCE_SYSTEM.MAPPING_ID;
  }

  @Override
  public Field<Integer> field8() {
    return SourceSystem.SOURCE_SYSTEM.VERSION;
  }

  @Override
  public Field<String> field9() {
    return SourceSystem.SOURCE_SYSTEM.CREATOR;
  }

  @Override
  public String component1() {
    return getId();
  }

  @Override
  public String component2() {
    return getName();
  }

  @Override
  public String component3() {
    return getEndpoint();
  }

  @Override
  public String component4() {
    return getDescription();
  }

  @Override
  public Instant component5() {
    return getCreated();
  }

  @Override
  public Instant component6() {
    return getDeleted();
  }

  @Override
  public String component7() {
    return getMappingId();
  }

  @Override
  public Integer component8() {
    return getVersion();
  }

  @Override
  public String component9() {
    return getCreator();
  }

  @Override
  public String value1() {
    return getId();
  }

  @Override
  public String value2() {
    return getName();
  }

  @Override
  public String value3() {
    return getEndpoint();
  }

  @Override
  public String value4() {
    return getDescription();
  }

  @Override
  public Instant value5() {
    return getCreated();
  }

  @Override
  public Instant value6() {
    return getDeleted();
  }

  @Override
  public String value7() {
    return getMappingId();
  }

  @Override
  public Integer value8() {
    return getVersion();
  }

  @Override
  public String value9() {
    return getCreator();
  }

  @Override
  public SourceSystemRecord value1(String value) {
    setId(value);
    return this;
  }

  @Override
  public SourceSystemRecord value2(String value) {
    setName(value);
    return this;
  }

  @Override
  public SourceSystemRecord value3(String value) {
    setEndpoint(value);
    return this;
  }

  @Override
  public SourceSystemRecord value4(String value) {
    setDescription(value);
    return this;
  }

  @Override
  public SourceSystemRecord value5(Instant value) {
    setCreated(value);
    return this;
  }

  @Override
  public SourceSystemRecord value6(Instant value) {
    setDeleted(value);
    return this;
  }

  @Override
  public SourceSystemRecord value7(String value) {
    setMappingId(value);
    return this;
  }

  @Override
  public SourceSystemRecord value8(Integer value) {
    setVersion(value);
    return this;
  }

  @Override
  public SourceSystemRecord value9(String value) {
    setCreator(value);
    return this;
  }

  @Override
  public SourceSystemRecord values(String value1, String value2, String value3, String value4,
      Instant value5, Instant value6, String value7, Integer value8, String value9) {
    value1(value1);
    value2(value2);
    value3(value3);
    value4(value4);
    value5(value5);
    value6(value6);
    value7(value7);
    value8(value8);
    value9(value9);
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached SourceSystemRecord
   */
  public SourceSystemRecord() {
    super(SourceSystem.SOURCE_SYSTEM);
  }

  /**
   * Create a detached, initialised SourceSystemRecord
   */
  public SourceSystemRecord(String id, String name, String endpoint, String description,
      Instant created, Instant deleted, String mappingId, Integer version, String creator) {
    super(SourceSystem.SOURCE_SYSTEM);

    setId(id);
    setName(name);
    setEndpoint(endpoint);
    setDescription(description);
    setCreated(created);
    setDeleted(deleted);
    setMappingId(mappingId);
    setVersion(version);
    setCreator(creator);
    resetChangedOnNotNull();
  }
}
