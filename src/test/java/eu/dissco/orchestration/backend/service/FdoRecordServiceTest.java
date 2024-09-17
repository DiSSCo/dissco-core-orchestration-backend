package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingHandleRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasHandleRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenRollbackCreationRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemHandleRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneRequestMas;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Slf4j
class FdoRecordServiceTest {

  private FdoRecordService builder;

  @BeforeEach
  void setup() {
    this.builder = new FdoRecordService(MAPPER, new FdoProperties());
  }

  @Test
  void testCreateRequestSourceSystem() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenSourceSystemRequest(), ObjectType.SOURCE_SYSTEM);

    // Then
    assertThat(result).isEqualTo(givenSourceSystemHandleRequest());
  }

  @Test
  void testCreateRequestMapping() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenDataMappingRequest(), ObjectType.DATA_MAPPING);

    // Then
    assertThat(result).isEqualTo(givenMappingHandleRequest());
  }

  @Test
  void testCreateRequestMas() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenMasRequest(), ObjectType.MAS);

    // Then
    assertThat(result).isEqualTo(givenMasHandleRequest());
  }

  @Test
  void testRollbackHandleCreation() throws Exception {
    // Given
    var expected = givenRollbackCreationRequest();

    // when
    var result = builder.buildRollbackCreateRequest(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testTombstonePid() throws Exception {
    // given
    var expected = givenTombstoneRequestMas();

    // When
    var result = builder.buildTombstoneRequest(ObjectType.MAS, BARE_HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

}
