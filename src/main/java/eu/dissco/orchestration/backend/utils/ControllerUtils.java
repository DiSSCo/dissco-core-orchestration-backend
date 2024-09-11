package eu.dissco.orchestration.backend.utils;

import eu.dissco.orchestration.backend.exception.ForbiddenException;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public class ControllerUtils {
  private ControllerUtils(){}

  public static Agent getAgent(Authentication authentication) throws ForbiddenException {
    var claims = ((Jwt) authentication.getPrincipal()).getClaims();
    if (claims.containsKey("orcid")) {
      StringBuilder fullName = new StringBuilder();
      if (claims.containsKey("given_name")){
        fullName.append(claims.get("given_name"));
      }
      if (claims.containsKey("family_name")){
        if (!fullName.isEmpty()) {
          fullName.append(" ");
        }
        fullName.append(claims.get("family_name"));
      }
      return new Agent()
          .withType(Type.SCHEMA_PERSON)
          .withSchemaName(fullName.toString())
          .withId((String) claims.get("orcid"));
    } else {
      log.error("Missing ORCID in token");
      throw new ForbiddenException("No ORCID provided");
    }
  }


}
