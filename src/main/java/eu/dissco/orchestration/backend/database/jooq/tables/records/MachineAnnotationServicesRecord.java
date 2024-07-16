/*
 * This file is generated by jOOQ.
 */
package eu.dissco.orchestration.backend.database.jooq.tables.records;


import eu.dissco.orchestration.backend.database.jooq.tables.MachineAnnotationServices;
import java.time.Instant;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class MachineAnnotationServicesRecord extends
    UpdatableRecordImpl<MachineAnnotationServicesRecord> {

  private static final long serialVersionUID = 1L;

  /**
   * Create a detached MachineAnnotationServicesRecord
   */
  public MachineAnnotationServicesRecord() {
    super(MachineAnnotationServices.MACHINE_ANNOTATION_SERVICES);
  }

  /**
   * Create a detached, initialised MachineAnnotationServicesRecord
   */
  public MachineAnnotationServicesRecord(String id, Integer version, String name, Instant created,
      String administrator, String containerImage, String containerImageTag,
      JSONB targetDigitalObjectFilters, String serviceDescription, String serviceState,
      String sourceCodeRepository, String serviceAvailability, String codeMaintainer,
      String codeLicense, String[] dependencies, String supportContact, String slaDocumentation,
      String topicname, Integer maxreplicas, Instant deletedOn, Boolean batchingPermitted,
      Integer timeToLive) {
    super(MachineAnnotationServices.MACHINE_ANNOTATION_SERVICES);

    setId(id);
    setVersion(version);
    setName(name);
    setCreated(created);
    setAdministrator(administrator);
    setContainerImage(containerImage);
    setContainerImageTag(containerImageTag);
    setTargetDigitalObjectFilters(targetDigitalObjectFilters);
    setServiceDescription(serviceDescription);
    setServiceState(serviceState);
    setSourceCodeRepository(sourceCodeRepository);
    setServiceAvailability(serviceAvailability);
    setCodeMaintainer(codeMaintainer);
    setCodeLicense(codeLicense);
    setDependencies(dependencies);
    setSupportContact(supportContact);
    setSlaDocumentation(slaDocumentation);
    setTopicname(topicname);
    setMaxreplicas(maxreplicas);
    setDeletedOn(deletedOn);
    setBatchingPermitted(batchingPermitted);
    setTimeToLive(timeToLive);
    resetChangedOnNotNull();
  }

  /**
   * Getter for <code>public.machine_annotation_services.id</code>.
   */
  public String getId() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.machine_annotation_services.id</code>.
   */
  public void setId(String value) {
    set(0, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.version</code>.
   */
  public Integer getVersion() {
    return (Integer) get(1);
  }

  /**
   * Setter for <code>public.machine_annotation_services.version</code>.
   */
  public void setVersion(Integer value) {
    set(1, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.name</code>.
   */
  public String getName() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.machine_annotation_services.name</code>.
   */
  public void setName(String value) {
    set(2, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.created</code>.
   */
  public Instant getCreated() {
    return (Instant) get(3);
  }

  /**
   * Setter for <code>public.machine_annotation_services.created</code>.
   */
  public void setCreated(Instant value) {
    set(3, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.administrator</code>.
   */
  public String getAdministrator() {
    return (String) get(4);
  }

  /**
   * Setter for <code>public.machine_annotation_services.administrator</code>.
   */
  public void setAdministrator(String value) {
    set(4, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.container_image</code>.
   */
  public String getContainerImage() {
    return (String) get(5);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.container_image</code>.
   */
  public void setContainerImage(String value) {
    set(5, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.container_image_tag</code>.
   */
  public String getContainerImageTag() {
    return (String) get(6);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.container_image_tag</code>.
   */
  public void setContainerImageTag(String value) {
    set(6, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.target_digital_object_filters</code>.
   */
  public JSONB getTargetDigitalObjectFilters() {
    return (JSONB) get(7);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.target_digital_object_filters</code>.
   */
  public void setTargetDigitalObjectFilters(JSONB value) {
    set(7, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.service_description</code>.
   */
  public String getServiceDescription() {
    return (String) get(8);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.service_description</code>.
   */
  public void setServiceDescription(String value) {
    set(8, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.service_state</code>.
   */
  public String getServiceState() {
    return (String) get(9);
  }

  /**
   * Setter for <code>public.machine_annotation_services.service_state</code>.
   */
  public void setServiceState(String value) {
    set(9, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.source_code_repository</code>.
   */
  public String getSourceCodeRepository() {
    return (String) get(10);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.source_code_repository</code>.
   */
  public void setSourceCodeRepository(String value) {
    set(10, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.service_availability</code>.
   */
  public String getServiceAvailability() {
    return (String) get(11);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.service_availability</code>.
   */
  public void setServiceAvailability(String value) {
    set(11, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.code_maintainer</code>.
   */
  public String getCodeMaintainer() {
    return (String) get(12);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.code_maintainer</code>.
   */
  public void setCodeMaintainer(String value) {
    set(12, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.code_license</code>.
   */
  public String getCodeLicense() {
    return (String) get(13);
  }

  /**
   * Setter for <code>public.machine_annotation_services.code_license</code>.
   */
  public void setCodeLicense(String value) {
    set(13, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.dependencies</code>.
   */
  public String[] getDependencies() {
    return (String[]) get(14);
  }

  /**
   * Setter for <code>public.machine_annotation_services.dependencies</code>.
   */
  public void setDependencies(String[] value) {
    set(14, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.support_contact</code>.
   */
  public String getSupportContact() {
    return (String) get(15);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.support_contact</code>.
   */
  public void setSupportContact(String value) {
    set(15, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.sla_documentation</code>.
   */
  public String getSlaDocumentation() {
    return (String) get(16);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.sla_documentation</code>.
   */
  public void setSlaDocumentation(String value) {
    set(16, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.topicname</code>.
   */
  public String getTopicname() {
    return (String) get(17);
  }

  /**
   * Setter for <code>public.machine_annotation_services.topicname</code>.
   */
  public void setTopicname(String value) {
    set(17, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.maxreplicas</code>.
   */
  public Integer getMaxreplicas() {
    return (Integer) get(18);
  }

  /**
   * Setter for <code>public.machine_annotation_services.maxreplicas</code>.
   */
  public void setMaxreplicas(Integer value) {
    set(18, value);
  }

  /**
   * Getter for <code>public.machine_annotation_services.deleted_on</code>.
   */
  public Instant getDeletedOn() {
    return (Instant) get(19);
  }

  /**
   * Setter for <code>public.machine_annotation_services.deleted_on</code>.
   */
  public void setDeletedOn(Instant value) {
    set(19, value);
  }

  /**
   * Getter for
   * <code>public.machine_annotation_services.batching_permitted</code>.
   */
  public Boolean getBatchingPermitted() {
    return (Boolean) get(20);
  }

  /**
   * Setter for
   * <code>public.machine_annotation_services.batching_permitted</code>.
   */
  public void setBatchingPermitted(Boolean value) {
    set(20, value);
  }

  // -------------------------------------------------------------------------
  // Primary key information
  // -------------------------------------------------------------------------

  /**
   * Getter for <code>public.machine_annotation_services.time_to_live</code>.
   */
  public Integer getTimeToLive() {
    return (Integer) get(21);
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Setter for <code>public.machine_annotation_services.time_to_live</code>.
   */
  public void setTimeToLive(Integer value) {
    set(21, value);
  }

  @Override
  public Record1<String> key() {
    return (Record1) super.key();
  }
}
