package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAS_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAS_TYPE_DOI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.TTL;
import static eu.dissco.orchestration.backend.testutils.TestUtils.UPDATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.flattenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasEnvironment;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasSecrets;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasSingleJsonApiWrapper;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneRequestMas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.properties.KubernetesProperties;
import eu.dissco.orchestration.backend.properties.MachineAnnotationServiceProperties;
import eu.dissco.orchestration.backend.repository.MachineAnnotationServiceRepository;
import eu.dissco.orchestration.backend.schema.EnvironmentalVariable;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
import eu.dissco.orchestration.backend.schema.SchemaContactPoint__1;
import eu.dissco.orchestration.backend.schema.SecretVariable;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Configuration;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIcreateNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIdeleteNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIreplaceNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.CustomObjectsApi.APIcreateNamespacedCustomObjectRequest;
import io.kubernetes.client.openapi.apis.CustomObjectsApi.APIdeleteNamespacedCustomObjectRequest;
import io.kubernetes.client.openapi.models.V1Deployment;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MachineAnnotationServiceServiceTest {

  private final Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
  private final KubernetesProperties kubernetesProperties = new KubernetesProperties();
  Clock updatedClock = Clock.fixed(UPDATED, ZoneOffset.UTC);
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
  @Mock
  private FdoProperties fdoProperties;
  private MachineAnnotationServiceService service;
  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  private static Stream<Arguments> masKeys() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(givenMasEnvironment(), givenMasSecrets()),
        Arguments.of(List.of(new EnvironmentalVariable("name", 1)),
            givenMasSecrets()),
        Arguments.of(List.of(new EnvironmentalVariable("name", true)),
            givenMasSecrets())
    );
  }

  @BeforeEach
  void setup() throws IOException {
    initTime();
    initFreeMaker();
    var kedaTemplate = configuration.getTemplate("keda-template.ftl");
    var deploymentTemplate = configuration.getTemplate("mas-template.ftl");
    service = new MachineAnnotationServiceService(handleComponent, fdoRecordService,
        kafkaPublisherService, repository, appsV1Api, customObjectsApi, kedaTemplate,
        deploymentTemplate, MAPPER, properties, kubernetesProperties, fdoProperties);
  }

  private void initFreeMaker() throws IOException {
    configuration.setDirectoryForTemplateLoading(new File("src/main/resources/templates/"));
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
    mockedClock.close();
  }

  @ParameterizedTest
  @MethodSource("masKeys")
  void testCreateMas(List<EnvironmentalVariable> masEnv,
      List<SecretVariable> masSecret) throws Exception {
    // Given
    var expectedMas = givenMas()
        .withOdsHasEnvironmentalVariables(masEnv)
        .withOdsHasSecretVariables(masSecret);
    var expected = new JsonApiWrapper(new JsonApiData(
        expectedMas.getId(),
        ObjectType.MAS,
        flattenMas(expectedMas)
    ), new JsonApiLinks(MAS_PATH));
    var masRequest = givenMasRequest()
        .withOdsHasEnvironmentalVariables(masEnv)
        .withOdsHasSecretVariables(masSecret);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    var result = service.createMachineAnnotationService(masRequest, givenAgent(), MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordService).should().buildCreateRequest(masRequest, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(expectedMas);
    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class));
    then(kafkaPublisherService).should()
        .publishCreateEvent(MAPPER.valueToTree(expectedMas), givenAgent());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = -1)
  void testCreateMasMinimum(Integer maxReplicas) throws Exception {
    // Given
    var mas = givenMas()
        .withSchemaContactPoint(new SchemaContactPoint__1())
        .withOdsTopicName(SUFFIX.toLowerCase())
        .withOdsMaxReplicas(1);
    var expected = new JsonApiWrapper(new JsonApiData(
        mas.getId(),
        ObjectType.MAS,
        flattenMas(mas)
    ), new JsonApiLinks(MAS_PATH));

    var masRequest =
        givenMasRequest()
            .withSchemaContactPoint(null)
            .withOdsTopicName(null)
            .withOdsMaxReplicas(maxReplicas);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    var result = service.createMachineAnnotationService(masRequest, givenAgent(), MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordService).should().buildCreateRequest(masRequest, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(mas);
    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class));
    then(kafkaPublisherService).should()
        .publishCreateEvent(MAPPER.valueToTree(mas), givenAgent());
  }

  @Test
  void testCreateMasHandleFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    willThrow(PidException.class).given(handleComponent).postHandle(any());

    // Then
    assertThrowsExactly(ProcessingFailedException.class, () ->
        service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));
  }

  @Test
  void testCreateMasDeployFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class)))
        .willReturn(createDeploy);
    given(createDeploy.execute()).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().createMachineAnnotationService(givenMas());
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(customObjectsApi).shouldHaveNoInteractions();
  }

  @Test
  void testCreateKedaFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace")).willReturn(deleteDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);
    given(createCustom.execute()).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().createMachineAnnotationService(givenMas());
    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class));
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"));
  }

  @Test
  void testCreateMasKafkaFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(MAPPER.valueToTree(givenMas()), givenAgent());
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace")).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMas());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
  }

  @Test
  void testCreateMasKafkaAndPidFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(MAPPER.valueToTree(givenMas()), givenAgent());
    willThrow(PidException.class).given(handleComponent).rollbackHandleCreation(any());
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace")).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMas());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
  }

  @Test
  void testUpdateMas() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper(2);
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq("namespace"), any(V1Deployment.class))).willReturn(replaceDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    var result = service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(appsV1Api).should()
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class));
    then(kafkaPublisherService).should()
        .publishUpdateEvent(MAPPER.valueToTree(givenMas(2)), MAPPER.valueToTree(prevMas.get()),
            givenAgent());
  }

  @Test
  void testUpdateMasDeployFails() throws Exception {
    // Given
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq("namespace"), any(V1Deployment.class))).willReturn(replaceDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);
    given(createCustom.execute()).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(repository).should().updateMachineAnnotationService(prevMas.get());
    then(appsV1Api).should(times(2))
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should(times(2))
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class));
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testUpdateKedaFails() throws ApiException {
    // Given
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq("namespace"), any(V1Deployment.class))).willReturn(replaceDeploy);
    given(replaceDeploy.execute()).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(repository).should().updateMachineAnnotationService(prevMas.get());
    then(appsV1Api).should()
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class));
    then(customObjectsApi).shouldHaveNoInteractions();
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testUpdateMasKafkaFails() throws Exception {
    // Given
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(MAPPER.valueToTree(givenMas(2)), MAPPER.valueToTree(prevMas.get()),
            givenAgent());
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq("namespace"), any(V1Deployment.class))).willReturn(replaceDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq("namespace"),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(appsV1Api).should(times(2))
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"),
            any(V1Deployment.class));
    then(customObjectsApi).should(times(2))
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should(times(2))
        .createNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            any(Object.class));
    then(repository).should().updateMachineAnnotationService(prevMas.get());
  }

  @Test
  void testUpdateMasEqual() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMas()));

    // When
    var result = service.updateMachineAnnotationService(HANDLE, mas, givenAgent(), MAS_PATH);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testUpdateMasNotFound() {
    // Given
    var mas = givenMasRequest();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(Optional.empty());

    // When/Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.updateMachineAnnotationService(HANDLE, mas, givenAgent(), MAS_PATH));
  }

  @Test
  void testGetMasById() {
    // Given
    var mas = givenMas();
    var expected = givenMasSingleJsonApiWrapper();
    given(repository.getMachineAnnotationService(HANDLE)).willReturn(mas);

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
    var machineAnnotationServices = Collections.nCopies(pageSize + 1, givenMas());
    given(repository.getMachineAnnotationServices(pageNum, pageSize)).willReturn(
        machineAnnotationServices);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, MAS_PATH);
    var expected = givenMasResponse(machineAnnotationServices.subList(0, pageSize), linksNode);

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
    var machineAnnotationServices = Collections.nCopies(pageSize, givenMas());
    given(repository.getMachineAnnotationServices(pageNum, pageSize)).willReturn(
        machineAnnotationServices);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, MAS_PATH);
    var expected = givenMasResponse(machineAnnotationServices.subList(0, pageSize), linksNode);

    // When
    var result = service.getMachineAnnotationServices(pageNum, pageSize, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testDeletedMasNotFound() {
    // Given
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.tombstoneMachineAnnotationService(BARE_HANDLE, givenAgent()));
  }

  @Test
  void testTombstoneMas()
      throws Exception {
    // Given
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(
        Optional.of(givenMas()));
    given(properties.getNamespace()).willReturn("namespace");
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace")).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject("keda.sh", "v1alpha1", "namespace",
        "scaledobjects", "gw0-pop-xsl-scaled-object")).willReturn(deleteCustom);
    mockedStatic.when(Instant::now).thenReturn(UPDATED);
    mockedClock.when(Clock::systemUTC).thenReturn(updatedClock);

    // When
    service.tombstoneMachineAnnotationService(BARE_HANDLE, givenAgent());

    // Then
    then(repository).should().tombstoneMachineAnnotationService(givenTombstoneMas(), Instant.now());
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq("namespace"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq("namespace"), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(handleComponent).should().tombstoneHandle(any(), eq(BARE_HANDLE));
    then(kafkaPublisherService).should().publishTombstoneEvent(any(), any(), eq(givenAgent()));
  }

  @Test
  void testTombstoneMasKafkaFailed() throws Exception {
    // Given
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(
        Optional.of(givenMas()));
    given(properties.getNamespace()).willReturn("namespace");
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace")).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject("keda.sh", "v1alpha1", "namespace",
        "scaledobjects", "gw0-pop-xsl-scaled-object")).willReturn(deleteCustom);
    doThrow(JsonProcessingException.class).when(kafkaPublisherService)
        .publishTombstoneEvent(any(), any(), eq(givenAgent()));
    given(fdoRecordService.buildTombstoneRequest(ObjectType.MAS, BARE_HANDLE)).willReturn(
        givenTombstoneRequestMas());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.tombstoneMachineAnnotationService(BARE_HANDLE, givenAgent()));

    // Then
    then(repository).should().tombstoneMachineAnnotationService(any(), any());
    then(handleComponent).should().tombstoneHandle(givenTombstoneRequestMas(), BARE_HANDLE);
  }

  @Test
  void testDeleteDeployFails() throws Exception {
    // Given
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(
        Optional.of(givenMas()));
    given(properties.getNamespace()).willReturn("namespace");
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace")).willReturn(deleteDeploy);
    given(deleteDeploy.execute()).willThrow(new ApiException());
    mockedStatic.when(Instant::now).thenReturn(UPDATED);
    mockedClock.when(Clock::systemUTC).thenReturn(updatedClock);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.tombstoneMachineAnnotationService(BARE_HANDLE, givenAgent()));

    // Then
    then(repository).should().getActiveMachineAnnotationService(BARE_HANDLE);
    then(repository).shouldHaveNoMoreInteractions();
    then(customObjectsApi).shouldHaveNoInteractions();
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testDeleteKedaFails() throws ApiException {
    // Given
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(
        Optional.of(givenMas()));
    given(properties.getNamespace()).willReturn("namespace");
    given(properties.getKafkaHost()).willReturn("kafka.svc.cluster.local:9092");
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(properties.getRunningEndpoint()).willReturn("https://dev.dissco.tech/api/running");
    given(appsV1Api.createNamespacedDeployment(eq("namespace"), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        "namespace")).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject("keda.sh", "v1alpha1", "namespace",
        "scaledobjects", "gw0-pop-xsl-scaled-object")).willReturn(deleteCustom);
    given(deleteCustom.execute()).willThrow(new ApiException());
    mockedStatic.when(Instant::now).thenReturn(UPDATED);
    mockedClock.when(Clock::systemUTC).thenReturn(updatedClock);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.tombstoneMachineAnnotationService(BARE_HANDLE, givenAgent()));

    // Then
    then(repository).should().getActiveMachineAnnotationService(BARE_HANDLE);
    then(repository).shouldHaveNoMoreInteractions();
    then(appsV1Api).should().deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq("namespace"));
    then(appsV1Api).should()
        .createNamespacedDeployment(eq("namespace"), any(V1Deployment.class));
    then(kafkaPublisherService).shouldHaveNoInteractions();
  }

  private Optional<MachineAnnotationService> buildOptionalPrev() {
    return Optional.of(givenMas(1, "Another MAS", TTL));
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
