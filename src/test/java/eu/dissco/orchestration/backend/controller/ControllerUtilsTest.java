package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.domain.AgentRoleType.CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import eu.dissco.orchestration.backend.exception.ForbiddenException;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.utils.AgentUtils;
import eu.dissco.orchestration.backend.utils.ControllerUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ControllerUtilsTest {

  @Mock
  private Authentication authentication;

  private static Stream<Arguments> claimsAndNames() {
    return Stream.of(
        Arguments.of(
            new HashMap<String, Object>(Map.of(
                "orcid", OBJECT_CREATOR,
                "family_name", "Attenborough",
                "given_name", "David")),
            AgentUtils.createAgent("David Attenborough", OBJECT_CREATOR, CREATOR, "orcid",
                Type.SCHEMA_PERSON)
        ),
        Arguments.of(new HashMap<String, Object>(Map.of(
                "orcid", OBJECT_CREATOR,
                "given_name", "David")),
            AgentUtils.createAgent("David", OBJECT_CREATOR, CREATOR, "orcid",
                Type.SCHEMA_PERSON)
        ),
        Arguments.of(new HashMap<String, Object>(Map.of(
                "orcid", OBJECT_CREATOR,
                "family_name", "Attenborough")),
            AgentUtils.createAgent("Attenborough", OBJECT_CREATOR, CREATOR, "orcid",
                Type.SCHEMA_PERSON)
        ));
  }

  @ParameterizedTest
  @MethodSource("claimsAndNames")
  void testGetAgentFullName(Map<String, Object> claims, Agent expected)
      throws eu.dissco.orchestration.backend.exception.ForbiddenException {
    // Given
    givenAuthentication(claims);

    // When
    var agent = ControllerUtils.getAgent(authentication, CREATOR);

    // Then
    assertThat(agent).isEqualTo(expected);
  }

  @Test
  void testNoOrcid() {
    // Given
    var claims = new HashMap<String, Object>(Map.of(
        "family_name", "Attenborough",
        "given_name", "David"));
    givenAuthentication(claims);

    // When / Then
    assertThrows(ForbiddenException.class, () -> ControllerUtils.getAgent(authentication, CREATOR));
  }

  private void givenAuthentication(Map<String, Object> claims) {
    var principal = mock(Jwt.class);
    given(authentication.getPrincipal()).willReturn(principal);
    given(principal.getClaims()).willReturn(claims);
  }
}
