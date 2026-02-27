package eu.dissco.orchestration.backend.web;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasHandleRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import eu.dissco.orchestration.backend.client.HandleClient;
import eu.dissco.orchestration.backend.exception.PidException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class HandleComponentTest {

  private HandleComponent handleComponent;
  @Mock
  HandleClient handleClient;

  @BeforeEach
  void setup() {
    handleComponent = new HandleComponent(handleClient);
  }

  @Test
  void testPostHandle() throws PidException {
    // Given
    given(handleClient.postHandle(List.of(givenMasHandleRequest()))).willReturn(
        givenHandleApiResponse());

    // When
    var result = handleComponent.postHandle(givenMasHandleRequest());

    // Then
    assertThat(result).isEqualTo(BARE_HANDLE);
  }

  @Test
  void testPostHandleFailed() {
    // Given
    doThrow(RuntimeException.class).when(handleClient).postHandle(any());

    // When / Then
    assertThrows(PidException.class, () -> handleComponent.postHandle(givenMasHandleRequest()));
  }

  @Test
  void testPostHandleBadResponse() {
    // Given
    given(handleClient.postHandle(any())).willReturn(MAPPER.createObjectNode());

    // When / Then
    assertThrows(PidException.class, () -> handleComponent.postHandle(givenMasHandleRequest()));

  }

  @Test
  void testTombstoneHandle() {
    // Given

    // When / Then
    assertDoesNotThrow(() -> handleComponent.tombstoneHandle(givenMasHandleRequest(), BARE_HANDLE));
  }

  @Test
  void testTombstoneHandleFails() {
    // Given
    doThrow(RuntimeException.class).when(handleClient).tombstoneHandle(any(), any());

    // When / Then
    assertThrows(PidException.class,
        () -> handleComponent.tombstoneHandle(givenMasHandleRequest(), BARE_HANDLE));
  }

  private JsonNode givenHandleApiResponse() {
    return MAPPER.readTree("""
        {
          "data":
          [
            {
              "type": "machineAnnotationService",
              "id":"20.5000.1025/GW0-POP-XSL",
              "attributes":{
              }
            }
          ]
        }""");
  }

}
