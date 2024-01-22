package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.service.MachineAnnotationServiceService.SUBJECT_TYPE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAS_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRecordResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasSingleJsonApiWrapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.KubernetesProperties;
import eu.dissco.orchestration.backend.properties.MachineAnnotationServiceProperties;
import eu.dissco.orchestration.backend.repository.MachineAnnotationServiceRepository;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Configuration;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Deployment;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MachineAnnotationServiceServiceTest {

  private final Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
  private final KubernetesProperties kubernetesProperties = new KubernetesProperties();
  @Mock
  private MachineAnnotationServiceRepository repository;
  @Mock
  private KafkaPublisherService kafkaPublisherService;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private FdoRecordService fdoRecordService;

  @Mock
  private MachineAnnotationServiceProperties properties;
  @Mock
  private AppsV1Api appsV1Api;
  @Mock
  private CustomObjectsApi customObjectsApi;
  private MachineAnnotationServiceService service;

  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  @BeforeEach
  void setup() throws IOException {
    initTime();

    initFreeMaker();
    var kedaTemplate = configuration.getTemplate("keda-template.ftl");
    var deploymentTemplate = configuration.getTemplate("mas-template.ftl");
    service = new MachineAnnotationServiceService(handleComponent, fdoRecordService, kafkaPublisherService, repository,
        appsV1Api, customObjectsApi, kedaTemplate, deploymentTemplate, MAPPER, properties,
        kubernetesProperties);
  }

  private void initFreeMaker() throws IOException {
    configuration.setDirectoryForTemplateLoading(new File("src/main/resources/templates/"));
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
    mockedClock.close();
  }

  @Test
  void testCreateMas() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper();
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");

    // When
    var result = service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class), eq(null), eq(null), eq(null));
    then(kafkaPublisherService).should()
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord()), SUBJECT_TYPE);
  }

  @Test
  void testCreateMasHandleFails() throws Exception {
    // Given
    var mas = givenMas();
    willThrow(PidCreationException.class).given(handleComponent).postHandle(any());

    // Then
    assertThrowsExactly(ProcessingFailedException.class, () ->
        service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));
  }

  @Test
  void testCreateMasDeployFails() throws Exception {
    // Given
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class), eq(null),
        eq(null), eq(null), eq(null))).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(customObjectsApi).shouldHaveNoInteractions();
  }

  @Test
  void testCreateKedaFails() throws Exception {
    // Given
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(),
        any(Object.class), eq(null), eq(null), eq(null))).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            eq(null), eq(null), eq(null), eq(null), eq(null), eq(null));
  }

  @Test
  void testCreateMasKafkaFails() throws Exception {
    // Given
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord()),
            SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class), eq(null), eq(null), eq(null));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            eq(null), eq(null), eq(null), eq(null), eq(null), eq(null));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"), eq(null), eq(null), eq(null), eq(null),
            eq(null));
  }

  @Test
  void testCreateMasKafkaAndPidFails() throws Exception {
    // Given
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord()),
            SUBJECT_TYPE);
    willThrow(PidCreationException.class).given(handleComponent).rollbackHandleCreation(any());


    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class), eq(null), eq(null), eq(null));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            eq(null), eq(null), eq(null), eq(null), eq(null), eq(null));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"), eq(null), eq(null), eq(null), eq(null),
            eq(null));
  }


  @Test
  void testUpdateMas() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper(2);
    var prevRecord = buildOptionalPrevRecord();
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(prevRecord);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");

    // When
    var result = service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateMachineAnnotationService(givenMasRecord(2));
    then(appsV1Api).should()
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"), eq(null), eq(null), eq(null), eq(null),
            eq(null));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class), eq(null), eq(null), eq(null));
    then(kafkaPublisherService).should()
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord(2)), givenJsonPatch(),
            SUBJECT_TYPE);
  }

  @Test
  void testUpdateMasDeployFails() throws Exception {
    // Given
    var prevRecord = buildOptionalPrevRecord();
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(prevRecord);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(),
        any(Object.class), eq(null), eq(null), eq(null))).willThrow(new ApiException())
        .willReturn(null);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMasRecord(2));
    then(repository).should().updateMachineAnnotationService(prevRecord.get());
    then(appsV1Api).should(times(2))
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"), eq(null), eq(null), eq(null), eq(null),
            eq(null));
    then(customObjectsApi).should(times(2))
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class), eq(null), eq(null), eq(null));
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testUpdateKedaFails() throws Exception {
    // Given
    var prevRecord = buildOptionalPrevRecord();
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(prevRecord);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq("namespace"),
        any(V1Deployment.class), eq(null), eq(null),
        eq(null), eq(null))).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMasRecord(2));
    then(repository).should().updateMachineAnnotationService(prevRecord.get());
    then(appsV1Api).should()
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(customObjectsApi).shouldHaveNoInteractions();
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testUpdateMasKafkaFails() throws Exception {
    // Given
    var prevRecord = buildOptionalPrevRecord();
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(prevRecord);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord(2)), givenJsonPatch(),
            SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMasRecord(2));
    then(appsV1Api).should(times(2))
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(customObjectsApi).should(times(2))
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"), eq(null), eq(null), eq(null), eq(null),
            eq(null));
    then(customObjectsApi).should(times(2))
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class), eq(null), eq(null), eq(null));
    then(repository).should().updateMachineAnnotationService(prevRecord.get());
  }

  private JsonNode givenJsonPatch() throws JsonProcessingException {
    return MAPPER.readTree("""
        [
          {
            "op": "replace",
            "path": "/maxReplicas",
            "value": 1
          },
          {
            "op": "replace",
            "path": "/sourceCodeRepository",
            "value": null
          },
          {
            "op": "replace",
            "path": "/supportContact",
            "value": null
          },
          {
            "op": "replace",
            "path": "/dependencies",
            "value": null
          },
          {
            "op": "replace",
            "path": "/slaDocumentation",
            "value": null
          },
          {
            "op": "replace",
            "path": "/codeLicense",
            "value": null
          },
          {
            "op": "replace",
            "path": "/serviceState",
            "value": null
          },
          {
            "op": "replace",
            "path": "/name",
            "value": "Another name"
          },
          {
            "op": "replace",
            "path": "/codeMaintainer",
            "value": null
          },
          {
            "op": "replace",
            "path": "/serviceAvailability",
            "value": null
          },
          {
            "op": "replace",
            "path": "/topicName",
            "value": "another-topic-name"
          },
          {
            "op": "replace",
            "path": "/serviceDescription",
            "value": null
          },
          {
            "op": "replace",
            "path": "/containerTag",
            "value": "less-fancy"
          }
        ]
        """);
  }

  @Test
  void testUpdateMasEqual() throws Exception {
    // Given
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMasRecord()));

    // When
    var result = service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testUpdateMasNotFound() {
    // Given
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(Optional.empty());

    // When/Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH));
  }

  @Test
  void testGetMasById() {
    // Given
    var masRecord = givenMasRecord();
    var expected = givenMasSingleJsonApiWrapper();
    given(repository.getMachineAnnotationService(HANDLE)).willReturn(masRecord);

    // When
    var result = service.getMachineAnnotationService(HANDLE, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMass() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    var masRecords = Collections.nCopies(pageSize + 1, givenMasRecord());
    given(repository.getMachineAnnotationServices(pageNum, pageSize)).willReturn(masRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, MAS_PATH);
    var expected = givenMasRecordResponse(masRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getMachineAnnotationServices(pageNum, pageSize, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMassSecondPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    var masRecords = Collections.nCopies(pageSize, givenMasRecord());
    given(repository.getMachineAnnotationServices(pageNum, pageSize)).willReturn(masRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, MAS_PATH);
    var expected = givenMasRecordResponse(masRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getMachineAnnotationServices(pageNum, pageSize, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testDeletedMas() {
    // Given
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMasRecord()));

    // When / Then
    assertDoesNotThrow(() -> service.deleteMachineAnnotationService(HANDLE));
  }

  @Test
  void testDeletedMasNotFound() {
    // Given
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.deleteMachineAnnotationService(HANDLE));
  }

  @Test
  void testDeleteMas() throws ApiException, NotFoundException {
    // Given
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMasRecord()));
    given(properties.getNamespace()).willReturn("namespace");

    // When
    service.deleteMachineAnnotationService(HANDLE);

    // Then
    then(repository).should().deleteMachineAnnotationService(HANDLE, Instant.now());
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            eq(null), eq(null), eq(null), eq(null), eq(null), eq(null));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"), eq(null), eq(null), eq(null), eq(null),
            eq(null));
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testDeleteDeployFails() throws ApiException {
    // Given
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMasRecord()));
    given(properties.getNamespace()).willReturn("namespace");
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace", null, null, null, null, null, null)).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.deleteMachineAnnotationService(HANDLE));

    // Then
    then(repository).should().deleteMachineAnnotationService(HANDLE, Instant.now());
    then(repository).should()
        .createMachineAnnotationService(any(MachineAnnotationServiceRecord.class));
    then(customObjectsApi).shouldHaveNoInteractions();
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testDeleteKedaFails() throws ApiException {
    // Given
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMasRecord()));
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(),
        eq(SUFFIX.toLowerCase() + "-scaled-object"), eq(null), eq(null), eq(null), eq(null),
        eq(null))).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.deleteMachineAnnotationService(HANDLE));

    // Then
    then(repository).should().deleteMachineAnnotationService(HANDLE, Instant.now());
    then(repository).should()
        .createMachineAnnotationService(any(MachineAnnotationServiceRecord.class));
    then(appsV1Api).should().deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq("namespace"),
        eq(null), eq(null), eq(null), eq(null), eq(null), eq(null));
    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class), eq(null), eq(null),
            eq(null), eq(null));
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  private Optional<MachineAnnotationServiceRecord> buildOptionalPrevRecord() {
    return Optional.of(new MachineAnnotationServiceRecord(
        HANDLE,
        1,
        CREATED,
        OBJECT_CREATOR,
        new MachineAnnotationService("Another name", "public.ecr.aws/dissco/fancy-mas",
            "less-fancy", MAPPER.createObjectNode(), null, null, null, null, null, null, null, null,
            null, "another-topic-name", 1, false),
        null
    ));
  }

  private void initTime() {
    Clock clock = Clock.fixed(CREATED, ZoneOffset.UTC);
    mockedClock = mockStatic(Clock.class);
    mockedClock.when(Clock::systemUTC).thenReturn(clock);
    Instant instant = Instant.now(clock);
    mockedStatic = mockStatic(Instant.class);
    mockedStatic.when(Instant::now).thenReturn(instant);
    mockedStatic.when(() -> Instant.from(any())).thenReturn(instant);
  }

}
