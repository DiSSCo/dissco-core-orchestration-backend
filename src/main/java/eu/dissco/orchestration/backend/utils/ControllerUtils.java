package eu.dissco.orchestration.backend.utils;

import static eu.dissco.orchestration.backend.utils.AgentUtils.createAgent;

import eu.dissco.orchestration.backend.domain.AgentRoleType;
import eu.dissco.orchestration.backend.exception.ForbiddenException;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public class ControllerUtils {

  public static final String ORCID = "orcid";
  public static final String PREFIX_OAS = "Prefix of target ID";
  public static final String SUFFIX_OAS = "Suffix of target ID";
  public static final String PAGE_NUM_OAS = "Desired page number";
  public static final String PAGE_SIZE_OAS = "Desired page size";
  public static final String DEFAULT_PAGE_NUM = "1";
  public static final String DEFAULT_PAGE_SIZE = "10";


  private ControllerUtils() {
  }

  public static Agent getAgent(Authentication authentication, AgentRoleType roleType)
      throws ForbiddenException {
    var claims = ((Jwt) authentication.getPrincipal()).getClaims();
    if (claims.containsKey(ORCID)) {
      StringBuilder fullName = new StringBuilder();
      if (claims.containsKey("given_name")) {
        fullName.append(claims.get("given_name"));
      }
      if (claims.containsKey("family_name")) {
        if (!fullName.isEmpty()) {
          fullName.append(" ");
        }
        fullName.append(claims.get("family_name"));
      }
      var nameString = fullName.isEmpty() ? null : fullName.toString();
      return createAgent(nameString, (String) claims.get(ORCID), roleType, ORCID,
          Type.SCHEMA_PERSON);
    } else {
      log.error("Missing ORCID in token");
      throw new ForbiddenException("No ORCID provided");
    }
  }


}
