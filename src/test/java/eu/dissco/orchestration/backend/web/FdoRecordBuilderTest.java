package eu.dissco.orchestration.backend.web;

import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
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
    assertThat(result).isEqualTo(expectedSourceSystemResponse());
  }

  @Test
  void testCreateRequestMapping() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenMapping(), ObjectType.MAPPING);

    // Then
    assertThat(result).isEqualTo(expectedMappingResponse());
  }

  @Test
  void testCreateRequestMas() throws Exception {
    // When
    var result = builder.buildCreateRequest(givenMas(), ObjectType.MAS);

    // Then
    assertThat(result).isEqualTo(expectedMasResponse());
  }

  private static JsonNode expectedMasResponse() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "machineAnnotationService",
            "attributes": {
              "fdoProfile": "http://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "issuedForAgent": "https://ror.org/0566bfb96",
              "digitalObjectType": "http://hdl.handle.net/21.T11148/64396cf36b976ad08267"
            }
          }
        }""");
  }

  private static JsonNode expectedSourceSystemResponse() throws Exception{
    return MAPPER.readTree("""
        {
          "data": {
            "type": "sourceSystem",
            "attributes": {
              "fdoProfile": "http://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "issuedForAgent": "https://ror.org/0566bfb96",
              "digitalObjectType": "http://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "sourceSystemName":"Naturalis Tunicate DWCA endpoint"
            }
          }
        }""");
  }

  private static JsonNode expectedMappingResponse() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "mapping",
            "attributes": {
              "fdoProfile": "http://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "issuedForAgent": "https://ror.org/0566bfb96",
              "digitalObjectType": "http://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "sourceDataStandard": "dwc"
            }
          }
        }""");
  }
}
