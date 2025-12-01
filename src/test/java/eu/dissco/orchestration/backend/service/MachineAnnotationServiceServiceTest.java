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
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIcreateNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIdeleteNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIlistNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIreplaceNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.apis.CustomObjectsApi.APIcreateNamespacedCustomObjectRequest;
import io.kubernetes.client.openapi.apis.CustomObjectsApi.APIdeleteNamespacedCustomObjectRequest;
import io.kubernetes.client.openapi.apis.CustomObjectsApi.APIlistNamespacedCustomObjectRequest;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1SecretKeySelector;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

  private static final String NAMESPACE = "machine-annotation-services";
  private static final Gson GSON = new Gson();
  private final Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
  private final KubernetesProperties kubernetesProperties = new KubernetesProperties();
  private final MachineAnnotationServiceProperties properties = new MachineAnnotationServiceProperties();
  private final ObjectMapper yamlMapper = new ObjectMapper(
      new YAMLFactory()).findAndRegisterModules();
  Clock updatedClock = Clock.fixed(UPDATED, ZoneOffset.UTC);
  @Mock
  private MachineAnnotationServiceRepository repository;
  @Mock
  private RabbitMqPublisherService rabbitMqPublisherService;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private FdoRecordService fdoRecordService;
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

  private static JsonElement givenKedaResource(String maxCount) {
    return GSON.fromJson(
        "{\"apiVersion\":\"keda.sh/v1alpha1\",\"items\":[{\"apiVersion\":\"keda.sh/v1alpha1\",\"kind\":\"ScaledObject\",\"metadata\":{\"name\":\"gw0-pop-xsl-scaled-object\",\"namespace\":\"machine-annotation-services\"},\"spec\":{\"maxReplicaCount\":"
            + maxCount
            + ",\"minReplicaCount\":0,\"scaleTargetRef\":{\"name\":\"gw0-pop-xsl-deployment\"},\"triggers\":[{\"authenticationRef\":{\"name\":\"keda-trigger-auth-rabbitmq-conn\"},\"metadata\":{\"mode\":\"QueueLength\",\"queueName\":\"mas-gw0-pop-xsl-queue\",\"value\":\"1.0\"},\"type\":\"rabbitmq\"}]}}]}",
        JsonElement.class);
  }

  private static JsonElement givenRabbitBinding(String routingKey) {
    return GSON.fromJson(
        "{\"apiVersion\":\"rabbitmq.com/v1beta1\",\"items\":[{\"apiVersion\":\"rabbitmq.com/v1beta1\",\"kind\":\"Binding\",\"metadata\":{\"name\":\"mas-gw0-pop-xsl-binding\",\"namespace\":\"machine-annotation-services\"},\"spec\":{\"destination\":\"mas-gw0-pop-xsl-queue\",\"destinationType\":\"queue\",\"rabbitmqClusterReference\":{\"name\":\"rabbitmq-cluster\",\"namespace\":\"rabbitmq\"},\"routingKey\":\""
            + routingKey + "\",\"source\":\"mas-exchange\",\"vhost\":\"/\"}}]}",
        JsonElement.class
    );
  }

  private static JsonElement givenRabbitQueue(String durable) {
    return GSON.fromJson(
        "{\"apiVersion\":\"rabbitmq.com/v1beta1\",\"items\":[{\"apiVersion\":\"rabbitmq.com/v1beta1\",\"kind\":\"Queue\",\"metadata\":{\"name\":\"mas-gw0-pop-xsl-queue\",\"namespace\":\"machine-annotation-services\"},\"spec\":{\"durable\":"
            + durable
            + ",\"name\":\"mas-gw0-pop-xsl-queue\",\"rabbitmqClusterReference\":{\"name\":\"rabbitmq-cluster\",\"namespace\":\"rabbitmq\"},\"type\":\"quorum\",\"deletionPolicy\": \"delete\",\"vhost\":\"/\"}}]}",
        JsonElement.class
    );
  }

  private static V1Deployment givenMasDeployment(String image, boolean addSecret) {
    var envList = new ArrayList<>(List.of(
        new V1EnvVar().name("MAS_NAME").value("A Machine Annotation Service"),
        new V1EnvVar().name("MAS_ID").value("20.5000.1025/GW0-POP-XSL"),
        new V1EnvVar().name("RABBITMQ_HOST")
            .value("rabbitmq-cluster.rabbitmq.svc.cluster.local"),
        new V1EnvVar().name("RABBITMQ_QUEUE").value("mas-fancy-topic-name-queue"),
        new V1EnvVar().name("RABBITMQ_USER")
            .valueFrom(new V1EnvVarSource().secretKeyRef(
                new V1SecretKeySelector().key("rabbitmq-username")
                    .name("aws-secrets"))),
        new V1EnvVar().name("RABBITMQ_PASSWORD")
            .valueFrom(new V1EnvVarSource().secretKeyRef(
                new V1SecretKeySelector().key("rabbitmq-password")
                    .name("aws-secrets"))),
        new V1EnvVar().name("RUNNING_ENDPOINT")
            .value("https://dev.dissco.tech/api/v1/mjr"),
        new V1EnvVar().name("server.port").value("8080"),
        new V1EnvVar().name("spring.datasource.password").valueFrom(
            new V1EnvVarSource().secretKeyRef(
                new V1SecretKeySelector().key("db-password").name("mas-secrets")))
    ));
    if (addSecret) {
      envList.add(
          new V1EnvVar().name("GEOPICK_USER").valueFrom(
              new V1EnvVarSource().secretKeyRef(
                  new V1SecretKeySelector().key("geopick-user").name("mas-secrets")))
      );
      envList.add(
          new V1EnvVar().name("GEOPICK_PASSWORD").valueFrom(
              new V1EnvVarSource().secretKeyRef(
                  new V1SecretKeySelector().key("geopick-password")
                      .name("mas-secrets")))
      );
    }

    return new V1Deployment().metadata(new V1ObjectMeta().name("gw0-pop-xsl-deployment"))
        .spec(new V1DeploymentSpec().template(new V1PodTemplateSpec().spec(
            new V1PodSpec().containers(List.of(
                new V1Container()
                    .name("gw0-pop-xsl")
                    .image(image)
                    .env(envList)
                    .volumeMounts(List.of(
                        new V1VolumeMount().name("temp-volume").mountPath("/temp"),
                        new V1VolumeMount().name("mas-secrets")
                            .mountPath("/mnt/secrets-store/mas-secrets").readOnly(true),
                        new V1VolumeMount().name("aws-secrets")
                            .mountPath("/mnt/secrets-store/aws-secrets").readOnly(true)
                    )))))));
  }

  @BeforeEach
  void setup() throws IOException {
    initTime();
    initFreeMaker();
    var kedaTemplate = configuration.getTemplate("keda-template.ftl");
    var deploymentTemplate = configuration.getTemplate("mas-template.ftl");
    var rabbitBindingTemplate = configuration.getTemplate("mas-rabbitmq-binding.ftl");
    var rabbitQueueTemplate = configuration.getTemplate("mas-rabbitmq-queue.ftl");
    properties.setRunningEndpoint("https://dev.dissco.tech/api/running");
    service = new MachineAnnotationServiceService(handleComponent, fdoRecordService,
        rabbitMqPublisherService, repository, appsV1Api, customObjectsApi, kedaTemplate,
        deploymentTemplate, rabbitBindingTemplate, rabbitQueueTemplate, MAPPER, yamlMapper,
        properties, kubernetesProperties, fdoProperties);
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
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    var result = service.createMachineAnnotationService(masRequest, givenAgent(), MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordService).should().buildCreateRequest(masRequest, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(expectedMas);
    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(customObjectsApi).should(times(3))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(rabbitMqPublisherService).should()
        .publishCreateEvent(expectedMas, givenAgent());
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
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    var result = service.createMachineAnnotationService(masRequest, givenAgent(), MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordService).should().buildCreateRequest(masRequest, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(mas);
    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(customObjectsApi).should(times(3))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(rabbitMqPublisherService).should()
        .publishCreateEvent(mas, givenAgent());
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
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
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
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);
    given(createCustom.execute()).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().createMachineAnnotationService(givenMas());
    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE));
  }

  @Test
  void testCreateMasRabbitBindingFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    given(createCustom.execute()).willReturn(mock(Object.class)).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMas());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(customObjectsApi).should(times(2))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
  }

  @Test
  void testCreateMasRabbitQueueFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-binding"))).willReturn(deleteCustom);
    given(createCustom.execute()).willReturn(mock(Object.class)).willReturn(mock(Object.class))
        .willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMas());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(customObjectsApi).should(times(3))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq("mas-" + SUFFIX.toLowerCase() + "-binding"));
  }

  @Test
  void testCreateMasEventFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    willThrow(JsonProcessingException.class).given(rabbitMqPublisherService)
        .publishCreateEvent(givenMas(), givenAgent());
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-binding"))).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-queue"))).willReturn(deleteCustom);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMas());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(customObjectsApi).should(times(3))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq("mas-" + SUFFIX.toLowerCase() + "-binding"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq("mas-" + SUFFIX.toLowerCase() + "-queue"));
  }

  @Test
  void testCreateMasEventAndPidFails() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    willThrow(JsonProcessingException.class).given(rabbitMqPublisherService)
        .publishCreateEvent(givenMas(), givenAgent());
    willThrow(PidException.class).given(handleComponent).rollbackHandleCreation(any());
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), any(Object.class))).willReturn(createCustom);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-binding"))).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-queue"))).willReturn(deleteCustom);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, givenAgent(), MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMas());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());

    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(customObjectsApi).should(times(3))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq("mas-" + SUFFIX.toLowerCase() + "-binding"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq("mas-" + SUFFIX.toLowerCase() + "-queue"));
  }

  @Test
  void testUpdateMas() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper(2);
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq(NAMESPACE), any(V1Deployment.class))).willReturn(replaceDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    var result = service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(appsV1Api).should()
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE),
            any(V1Deployment.class));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should()
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(rabbitMqPublisherService).should()
        .publishUpdateEvent(givenMas(2), prevMas.get(), givenAgent());
  }

  @Test
  void testUpdateMasDeployFails() throws Exception {
    // Given
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq(NAMESPACE), any(V1Deployment.class))).willReturn(replaceDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);
    given(createCustom.execute()).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(repository).should().updateMachineAnnotationService(prevMas.get());
    then(appsV1Api).should(times(2))
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE),
            any(V1Deployment.class));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should(times(2))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            any(Object.class));
    then(rabbitMqPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testUpdateKedaFails() throws ApiException {
    // Given
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq(NAMESPACE), any(V1Deployment.class))).willReturn(replaceDeploy);
    given(replaceDeploy.execute()).willThrow(new ApiException());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(repository).should().updateMachineAnnotationService(prevMas.get());
    then(appsV1Api).should()
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE),
            any(V1Deployment.class));
    then(customObjectsApi).shouldHaveNoInteractions();
    then(rabbitMqPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testUpdateMasEventFails() throws Exception {
    // Given
    var prevMas = buildOptionalPrev();
    var mas = givenMasRequest();
    given(fdoProperties.getMasType()).willReturn(MAS_TYPE_DOI);
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(prevMas);
    willThrow(JsonProcessingException.class).given(rabbitMqPublisherService)
        .publishUpdateEvent(givenMas(2), prevMas.get(), givenAgent());
    var replaceDeploy = mock(APIreplaceNamespacedDeploymentRequest.class);
    given(appsV1Api.replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"),
        eq(NAMESPACE), any(V1Deployment.class))).willReturn(replaceDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), eq(SUFFIX.toLowerCase() + "-scaled-object"))).willReturn(deleteCustom);
    var createCustom = mock(APIcreateNamespacedCustomObjectRequest.class);
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE),
        anyString(), any(Object.class))).willReturn(createCustom);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(BARE_HANDLE, mas, givenAgent(), MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMas(2));
    then(appsV1Api).should(times(2))
        .replaceNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE),
            any(V1Deployment.class));
    then(customObjectsApi).should(times(2))
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(customObjectsApi).should(times(2))
        .createNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
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
  void testGetMasById() throws NotFoundException {
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
  void testGetMasNotFound(){
    // Given

    // When / Then
    assertThrows(NotFoundException.class, () -> service.getMachineAnnotationService(HANDLE, MAS_PATH));
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
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject("keda.sh", "v1alpha1", NAMESPACE,
        "scaledobjects", "gw0-pop-xsl-scaled-object")).willReturn(deleteCustom);
    mockedStatic.when(Instant::now).thenReturn(UPDATED);
    mockedClock.when(Clock::systemUTC).thenReturn(updatedClock);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-binding"))).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-queue"))).willReturn(deleteCustom);

    // When
    service.tombstoneMachineAnnotationService(BARE_HANDLE, givenAgent());

    // Then
    then(repository).should().tombstoneMachineAnnotationService(givenTombstoneMas(), Instant.now());
    then(appsV1Api).should()
        .deleteNamespacedDeployment(eq(SUFFIX.toLowerCase() + "-deployment"), eq(NAMESPACE));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq(SUFFIX.toLowerCase() + "-scaled-object"));
    then(handleComponent).should().tombstoneHandle(any(), eq(BARE_HANDLE));
    then(rabbitMqPublisherService).should().publishTombstoneEvent(any(), any(), eq(givenAgent()));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq("mas-" + SUFFIX.toLowerCase() + "-binding"));
    then(customObjectsApi).should()
        .deleteNamespacedCustomObject(anyString(), anyString(), eq(NAMESPACE), anyString(),
            eq("mas-" + SUFFIX.toLowerCase() + "-queue"));
  }

  @Test
  void testTombstoneMasEventFailed() throws Exception {
    // Given
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(
        Optional.of(givenMas()));
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject("keda.sh", "v1alpha1", NAMESPACE,
        "scaledobjects", "gw0-pop-xsl-scaled-object")).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-binding"))).willReturn(deleteCustom);
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE),
        anyString(), eq("mas-" + SUFFIX.toLowerCase() + "-queue"))).willReturn(deleteCustom);
    doThrow(JsonProcessingException.class).when(rabbitMqPublisherService)
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
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
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
    then(rabbitMqPublisherService).shouldHaveNoInteractions();
  }

  @Test
  void testDeleteKedaFails() throws ApiException {
    // Given
    given(repository.getActiveMachineAnnotationService(BARE_HANDLE)).willReturn(
        Optional.of(givenMas()));
    var createDeploy = mock(APIcreateNamespacedDeploymentRequest.class);
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class)))
        .willReturn(createDeploy);
    var deleteDeploy = mock(APIdeleteNamespacedDeploymentRequest.class);
    given(appsV1Api.deleteNamespacedDeployment(SUFFIX.toLowerCase() + "-deployment",
        NAMESPACE)).willReturn(deleteDeploy);
    var deleteCustom = mock(APIdeleteNamespacedCustomObjectRequest.class);
    given(customObjectsApi.deleteNamespacedCustomObject("keda.sh", "v1alpha1", NAMESPACE,
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
        eq(NAMESPACE));
    then(appsV1Api).should()
        .createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
    then(rabbitMqPublisherService).shouldHaveNoInteractions();
  }

  private Optional<MachineAnnotationService> buildOptionalPrev() {
    return Optional.of(givenMas(1, "Another MAS", TTL));
  }

  @Test
  void synchronizeMissingResources()
      throws ApiException, TemplateException, IOException, InterruptedException {
    // Given
    var deployResponse = mock(APIlistNamespacedDeploymentRequest.class);
    var customResponse = mock(APIlistNamespacedCustomObjectRequest.class);
    given(customResponse.execute()).willReturn(
        GSON.fromJson("{ \"items\": []}", JsonElement.class));
    given(appsV1Api.listNamespacedDeployment(NAMESPACE)).willReturn(deployResponse);
    given(customObjectsApi.listNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString())).willReturn(customResponse);
    given(deployResponse.execute()).willReturn(new V1DeploymentList());
    given(repository.getMachineAnnotationServices(anyInt(), anyInt())).willReturn(
        List.of(givenMas()));
    given(appsV1Api.createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class))).willReturn(
        mock(APIcreateNamespacedDeploymentRequest.class));
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), any(Object.class))).willReturn(
        mock(APIcreateNamespacedCustomObjectRequest.class));

    // When
    service.setup();

    //Then
    then(appsV1Api).should().createNamespacedDeployment(eq(NAMESPACE), any(V1Deployment.class));
  }

  @Test
  void synchronizeExcessMas()
      throws ApiException, TemplateException, IOException, InterruptedException {
    // Given
    var deployResponse = mock(APIlistNamespacedDeploymentRequest.class);
    var customKedaResponse = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sKeda = givenKedaResource("5.0");
    given(customKedaResponse.execute()).willReturn(k8sKeda);
    var customRabbitBinding = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sRabbitBinding = givenRabbitBinding("gw0-pop-xsl");
    given(customRabbitBinding.execute()).willReturn(k8sRabbitBinding);
    var customRabbitQueue = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sRabbitQueue = givenRabbitQueue("true");
    given(customRabbitQueue.execute()).willReturn(k8sRabbitQueue);
    given(appsV1Api.listNamespacedDeployment(NAMESPACE)).willReturn(deployResponse);
    given(customObjectsApi.listNamespacedCustomObject(eq("keda.sh"), anyString(),
        eq(NAMESPACE), anyString())).willReturn(customKedaResponse);
    given(customObjectsApi.listNamespacedCustomObject(eq("rabbitmq.com"), anyString(),
        eq(NAMESPACE), eq("bindings"))).willReturn(customRabbitBinding);
    given(customObjectsApi.listNamespacedCustomObject(eq("rabbitmq.com"), anyString(),
        eq(NAMESPACE), eq("queues"))).willReturn(customRabbitQueue);
    given(deployResponse.execute()).willReturn(
        new V1DeploymentList().addItemsItem(
            givenMasDeployment("public.ecr.aws/dissco/fancy-mas:sha-54289", true)));
    given(repository.getMachineAnnotationServices(anyInt(), anyInt())).willReturn(List.of());
    given(appsV1Api.deleteNamespacedDeployment(anyString(), eq(NAMESPACE))).willReturn(
        mock(APIdeleteNamespacedDeploymentRequest.class));
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), anyString())).willReturn(
        mock(APIdeleteNamespacedCustomObjectRequest.class));

    // When
    service.setup();

    //Then
    then(appsV1Api).should().deleteNamespacedDeployment(anyString(), eq(NAMESPACE));
    then(customObjectsApi).should(times(3)).deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), anyString());
  }

  @Test
  void synchronizeOutOfSyncMas()
      throws ApiException, TemplateException, IOException, InterruptedException {
    // Given
    given(repository.getMachineAnnotationServices(anyInt(), anyInt())).willReturn(
        List.of(givenMas()));
    var deployResponse = mock(APIlistNamespacedDeploymentRequest.class);
    var customKedaResponse = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sKeda = givenKedaResource("1.0");
    given(customKedaResponse.execute()).willReturn(k8sKeda);
    var customRabbitBinding = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sRabbitBinding = givenRabbitBinding("another-routing-key");
    given(customRabbitBinding.execute()).willReturn(k8sRabbitBinding);
    var customRabbitQueue = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sRabbitQueue = givenRabbitQueue("false");
    given(customRabbitQueue.execute()).willReturn(k8sRabbitQueue);
    given(appsV1Api.listNamespacedDeployment(NAMESPACE)).willReturn(deployResponse);
    given(customObjectsApi.listNamespacedCustomObject(eq("keda.sh"), anyString(),
        eq(NAMESPACE), anyString())).willReturn(customKedaResponse);
    given(customObjectsApi.listNamespacedCustomObject(eq("rabbitmq.com"), anyString(),
        eq(NAMESPACE), eq("bindings"))).willReturn(customRabbitBinding);
    given(customObjectsApi.listNamespacedCustomObject(eq("rabbitmq.com"), anyString(),
        eq(NAMESPACE), eq("queues"))).willReturn(customRabbitQueue);
    given(deployResponse.execute()).willReturn(
        new V1DeploymentList().addItemsItem(givenMasDeployment("anotherImage", true)));
    given(appsV1Api.replaceNamespacedDeployment(anyString(), eq(NAMESPACE), any(
        V1Deployment.class))).willReturn(
        mock(APIreplaceNamespacedDeploymentRequest.class));
    given(customObjectsApi.deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), anyString())).willReturn(
        mock(APIdeleteNamespacedCustomObjectRequest.class));
    given(customObjectsApi.createNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), any(Object.class))).willReturn(
        mock(APIcreateNamespacedCustomObjectRequest.class));

    // When
    service.setup();

    //Then
    then(appsV1Api).should().replaceNamespacedDeployment(anyString(), eq(NAMESPACE), any(
        V1Deployment.class));
    then(customObjectsApi).should(times(3)).deleteNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), anyString());
    then(customObjectsApi).should(times(3)).createNamespacedCustomObject(anyString(), anyString(),
        eq(NAMESPACE), anyString(), any(Object.class));
  }

  @Test
  void synchronizeInSyncMas()
      throws ApiException, TemplateException, IOException, InterruptedException {
    // Given
    given(repository.getMachineAnnotationServices(anyInt(), anyInt())).willReturn(
        List.of(givenMas()));
    var deployResponse = mock(APIlistNamespacedDeploymentRequest.class);
    var customKedaResponse = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sKeda = givenKedaResource("5.0");
    given(customKedaResponse.execute()).willReturn(k8sKeda);
    var customRabbitBinding = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sRabbitBinding = givenRabbitBinding("gw0-pop-xsl");
    given(customRabbitBinding.execute()).willReturn(k8sRabbitBinding);
    var customRabbitQueue = mock(APIlistNamespacedCustomObjectRequest.class);
    var k8sRabbitQueue = givenRabbitQueue("true");
    given(customRabbitQueue.execute()).willReturn(k8sRabbitQueue);
    given(appsV1Api.listNamespacedDeployment(NAMESPACE)).willReturn(deployResponse);
    given(customObjectsApi.listNamespacedCustomObject(eq("keda.sh"), anyString(),
        eq(NAMESPACE), anyString())).willReturn(customKedaResponse);
    given(customObjectsApi.listNamespacedCustomObject(eq("rabbitmq.com"), anyString(),
        eq(NAMESPACE), eq("bindings"))).willReturn(customRabbitBinding);
    given(customObjectsApi.listNamespacedCustomObject(eq("rabbitmq.com"), anyString(),
        eq(NAMESPACE), eq("queues"))).willReturn(customRabbitQueue);
    given(deployResponse.execute()).willReturn(
        new V1DeploymentList().addItemsItem(
            givenMasDeployment("public.ecr.aws/dissco/fancy-mas:sha-54289", false)));

    // When
    service.setup();

    //Then
    then(appsV1Api).shouldHaveNoMoreInteractions();
    then(customObjectsApi).shouldHaveNoMoreInteractions();
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
