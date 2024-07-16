/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq.tables.records;


import eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType;
import eu.dissco.orchestration.backend.database.jooq.tables.NewSourceSystem;
import java.time.Instant;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class NewSourceSystemRecord extends UpdatableRecordImpl<NewSourceSystemRecord> {

  private static final long serialVersionUID = 1L;

  /**
   * Create a detached NewSourceSystemRecord
   */
  public NewSourceSystemRecord() {
    super(NewSourceSystem.NEW_SOURCE_SYSTEM);
  }

  /**
   * Create a detached, initialised NewSourceSystemRecord
   */
  public NewSourceSystemRecord(String id, String name, String endpoint, Instant dateCreated,
      Instant dateModified, Instant dateTombstoned, String mappingId, Integer version,
      String creator, TranslatorType translatorType, JSONB data) {
    super(NewSourceSystem.NEW_SOURCE_SYSTEM);

    setId(id);
    setName(name);
    setEndpoint(endpoint);
    setDateCreated(dateCreated);
    setDateModified(dateModified);
    setDateTombstoned(dateTombstoned);
    setMappingId(mappingId);
    setVersion(version);
    setCreator(creator);
    setTranslatorType(translatorType);
    setData(data);
    resetChangedOnNotNull();
  }

  /**
   * Getter for <code>public.new_source_system.id</code>.
   */
  public String getId() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.new_source_system.id</code>.
   */
  public void setId(String value) {
    set(0, value);
  }

  /**
   * Getter for <code>public.new_source_system.name</code>.
   */
  public String getName() {
    return (String) get(1);
  }

  /**
   * Setter for <code>public.new_source_system.name</code>.
   */
  public void setName(String value) {
    set(1, value);
  }

  /**
   * Getter for <code>public.new_source_system.endpoint</code>.
   */
  public String getEndpoint() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.new_source_system.endpoint</code>.
   */
  public void setEndpoint(String value) {
    set(2, value);
  }

  /**
   * Getter for <code>public.new_source_system.date_created</code>.
   */
  public Instant getDateCreated() {
    return (Instant) get(3);
  }

  /**
   * Setter for <code>public.new_source_system.date_created</code>.
   */
  public void setDateCreated(Instant value) {
    set(3, value);
  }

  /**
   * Getter for <code>public.new_source_system.date_modified</code>.
   */
  public Instant getDateModified() {
    return (Instant) get(4);
  }

  /**
   * Setter for <code>public.new_source_system.date_modified</code>.
   */
  public void setDateModified(Instant value) {
    set(4, value);
  }

  /**
   * Getter for <code>public.new_source_system.date_tombstoned</code>.
   */
  public Instant getDateTombstoned() {
    return (Instant) get(5);
  }

  /**
   * Setter for <code>public.new_source_system.date_tombstoned</code>.
   */
  public void setDateTombstoned(Instant value) {
    set(5, value);
  }

  /**
   * Getter for <code>public.new_source_system.mapping_id</code>.
   */
  public String getMappingId() {
    return (String) get(6);
  }

  /**
   * Setter for <code>public.new_source_system.mapping_id</code>.
   */
  public void setMappingId(String value) {
    set(6, value);
  }

  /**
   * Getter for <code>public.new_source_system.version</code>.
   */
  public Integer getVersion() {
    return (Integer) get(7);
  }

  /**
   * Setter for <code>public.new_source_system.version</code>.
   */
  public void setVersion(Integer value) {
    set(7, value);
  }

  /**
   * Getter for <code>public.new_source_system.creator</code>.
   */
  public String getCreator() {
    return (String) get(8);
  }

  /**
   * Setter for <code>public.new_source_system.creator</code>.
   */
  public void setCreator(String value) {
    set(8, value);
  }

  /**
   * Getter for <code>public.new_source_system.translator_type</code>.
   */
  public TranslatorType getTranslatorType() {
    return (TranslatorType) get(9);
  }

  /**
   * Setter for <code>public.new_source_system.translator_type</code>.
   */
  public void setTranslatorType(TranslatorType value) {
    set(9, value);
  }

  // -------------------------------------------------------------------------
  // Primary key information
  // -------------------------------------------------------------------------

  /**
   * Getter for <code>public.new_source_system.data</code>.
   */
  public JSONB getData() {
    return (JSONB) get(10);
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Setter for <code>public.new_source_system.data</code>.
   */
  public void setData(JSONB value) {
    set(10, value);
  }

  @Override
  public Record1<String> key() {
    return (Record1) super.key();
  }
}
