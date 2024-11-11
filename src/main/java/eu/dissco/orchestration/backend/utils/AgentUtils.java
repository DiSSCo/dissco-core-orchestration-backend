package eu.dissco.orchestration.backend.utils;

import static eu.dissco.orchestration.backend.schema.Identifier.DctermsType.DOI;
import static eu.dissco.orchestration.backend.schema.Identifier.DctermsType.HANDLE;
import static eu.dissco.orchestration.backend.schema.Identifier.OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT;
import static eu.dissco.orchestration.backend.schema.Identifier.OdsIdentifierStatus.PREFERRED;

import eu.dissco.orchestration.backend.domain.AgentRoleType;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.schema.Identifier;
import eu.dissco.orchestration.backend.schema.Identifier.DctermsType;
import eu.dissco.orchestration.backend.schema.OdsHasRole;
import java.util.List;

public class AgentUtils {

  private AgentUtils() {
  }

  public static eu.dissco.orchestration.backend.schema.Agent createMachineAgent(String name,
      String pid, AgentRoleType role, String idTitle, Type agentType) {
    var agent = new Agent()
        .withType(agentType)
        .withId(pid)
        .withSchemaName(name)
        .withSchemaIdentifier(pid)
        .withOdsHasRoles(List.of(new OdsHasRole().withType("schema:Role")
            .withSchemaRoleName(role.getName())));
    if (pid != null) {
      var identifier = new Identifier()
          .withType("ods:Identifier")
          .withId(pid)
          .withDctermsIdentifier(pid)
          .withOdsIsPartOfLabel(false)
          .withOdsIdentifierStatus(PREFERRED)
          .withOdsGupriLevel(
              GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT);
      if (idTitle.equals(DOI.value())) {
        identifier.withDctermsType(DOI);
        identifier.withDctermsTitle("DOI");
      } else if (idTitle.equals(HANDLE.value())) {
        identifier.withDctermsType(HANDLE);
        identifier.withDctermsTitle("HANDLE");
      } else if (idTitle.equals("orcid")) {
        identifier.withDctermsType(DctermsType.URL);
        identifier.withDctermsTitle("ORCID");
      }
      agent.setOdsHasIdentifiers(List.of(identifier));
    }
    return agent;
  }
}
