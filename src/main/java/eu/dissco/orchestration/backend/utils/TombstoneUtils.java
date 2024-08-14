package eu.dissco.orchestration.backend.utils;

import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.schema.TombstoneMetadata;
import java.time.Instant;
import java.util.Date;

public class TombstoneUtils {

  private TombstoneUtils() {
    // This is a utility class
  }

  public static TombstoneMetadata buildTombstoneMetadata(String userID, String text) {
    return new TombstoneMetadata()
        .withType("ods:TombstoneMetadata")
        .withOdsTombstonedByAgent(new Agent().withType(Type.SCHEMA_PERSON).withId(userID))
        .withOdsTombstoneDate(Date.from(Instant.now()))
        .withOdsTombstoneText(text);
  }
}
