package eu.dissco.orchestration.backend.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {
  public static final String SS_NAME = "Naturalis Tunicate DWCA endpoint";
  public static final String PREFIX = "20.5000.1025";
  public static final String SUFFIX = "GW0-POP-XSL";
  public static final String HANDLE = PREFIX + "/" + SUFFIX;
  public static final String HANDLE_ALT = PREFIX + "/EMK-X81-1QZ";
  public static final String SS_ENDPOINT = "https://api.biodiversitydata.nl/v2/specimen/dwca/getDataSet/tunicata";
  public static final String SS_DESCRIPTION = "Source system for the DWCA of the Tunicate specimen";
  public static final Instant CREATED = Instant.parse("2022-11-01T09:59:24.00Z");
  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  public static final String SANDBOX_URI = "https://sandbox.dissco.tech/";


  private TestUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static SourceSystemRecord givenSourceSystemRecord(){
    return new SourceSystemRecord(
        HANDLE,
        CREATED,
        givenSourceSystem()
    );
  }

  public static SourceSystem givenSourceSystem(){
    return new SourceSystem(
        SS_NAME,
        SS_ENDPOINT,
        SS_DESCRIPTION,
        HANDLE_ALT
    );
  }

  public static JsonApiWrapper givenSsRecordResponse(List<SourceSystemRecord> ssRecords, JsonApiLinks linksNode){
    List<JsonApiData> dataNode = new ArrayList<>();
    ssRecords.forEach(ss -> dataNode.add(new JsonApiData(ss.id(), HandleType.SOURCE_SYSTEM, MAPPER.valueToTree(ss))));
    return new JsonApiWrapper(dataNode, linksNode);
  }
  
  public static JsonApiLinks givenLinksNode(String path, int pageNum, int pageSize, boolean hasNext){
    String pn = "?pageNumber=";
    String ps = "&pageSize=";
    String self = path + pn + pageNum + ps + pageSize;
    String first = path + pn + "1" + ps + pageSize;
    String prev = (pageNum <= 1) ? null : path + pn + (pageNum - 1) + ps + pageSize;
    String next =
        (hasNext) ? path + pn + (pageNum + 1) + ps + pageSize : null;
    return new JsonApiLinks(self, first, next, prev);
  }


}
