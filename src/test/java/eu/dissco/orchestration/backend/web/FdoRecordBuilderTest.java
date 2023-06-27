package eu.dissco.orchestration.backend.web;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.expectedHandleRollbackUpdate;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingHandleRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasHandleRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemHandleRequest;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.ObjectType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Slf4j
class FdoRecordBuilderTest {

  private FdoRecordBuilder builder;

  @BeforeEach
  void setup() {
    this.builder = new FdoRecordBuilder(MAPPER);
  }
  @Test
  void testCreateRequestSourceSystem() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenSourceSystem(), ObjectType.SOURCE_SYSTEM);

    // Then
    assertThat(result).isEqualTo(givenSourceSystemHandleRequest());
  }

  @Test
  void testCreateRequestMapping() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenMapping(), ObjectType.MAPPING);

    // Then
    assertThat(result).isEqualTo(givenMappingHandleRequest());
  }

  @Test
  void testCreateRequestMas() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenMas(), ObjectType.MAS);

    // Then
    assertThat(result).isEqualTo(givenMasHandleRequest());
  }

  @Test
  void testRollbackHandleUpdate() throws Exception{
    // When
    var result = builder.buildRollbackUpdateRequest(givenMas(), ObjectType.MAS, HANDLE);

    // Then
    assertThat(result).isEqualTo(expectedHandleRollbackUpdate());
  }

  @Test
  void testRollbackHandleCreation() throws Exception {
    // Given
    var expected = MAPPER.readTree("""
        {
          "data":
            [{"id":"20.5000.1025/GW0-POP-XSL"}]
        }""");

    // when
    var result = builder.buildRollbackCreateRequest(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);

  }

}
