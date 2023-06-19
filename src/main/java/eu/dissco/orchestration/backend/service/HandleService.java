package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleAttribute;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.repository.HandleRepository;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleService {

  private static final String PREFIX = "20.5000.1025/";
  private final Random random;
  private final char[] symbols = "ABCDEFGHJKLMNPQRSTUVWXYZ1234567890".toCharArray();
  private final char[] buffer = new char[11];
  private final ObjectMapper mapper;
  private final DocumentBuilder documentBuilder;
  private final HandleRepository repository;
  private final ApplicationProperties appProperties;

  public String createNewHandle(HandleType type)
      throws TransformerException {
    var handle = generateHandle();
    var recordTimestamp = Instant.now();
    var handleAttributes = fillPidRecord(handle, recordTimestamp, type);
    repository.createHandle(handle, recordTimestamp, handleAttributes);
    return handle;
  }

  private List<HandleAttribute> fillPidRecord(String handle, Instant recordTimestamp,
      HandleType type)
      throws TransformerException {
    var handleAttributes = new ArrayList<HandleAttribute>();
    handleAttributes.add(new HandleAttribute(1, "pid",
        ("https://hdl.handle.net/" + handle).getBytes(StandardCharsets.UTF_8)));
    handleAttributes.add(new HandleAttribute(2, "pidIssuer",
        createPidReference("https://doi.org/10.22/10.22/2AA-GAA-E29", "DOI", "RA Issuing DOI")));
    handleAttributes.add(new HandleAttribute(3, "digitalObjectType",
        createPidReference("https://hdl.handle.net/21...", "Handle", type.toString())));
    handleAttributes.add(
        new HandleAttribute(5, "10320/loc", createLocations(handle, type)));
    handleAttributes.add(new HandleAttribute(6, "issueDate", createIssueDate(recordTimestamp)));
    handleAttributes.add(
        new HandleAttribute(7, "issueNumber", "1".getBytes(StandardCharsets.UTF_8)));
    handleAttributes.add(
        new HandleAttribute(8, "pidStatus", "DRAFT".getBytes(StandardCharsets.UTF_8)));
    handleAttributes.add(
        new HandleAttribute(11, "pidKernelMetadataLicense",
            "https://creativecommons.org/publicdomain/zero/1.0/".getBytes(StandardCharsets.UTF_8)));
    handleAttributes.add(new HandleAttribute(100, "HS_ADMIN", decodeAdmin()));
    return handleAttributes;
  }

  private byte[] createIssueDate(Instant recordTimestamp) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    return formatter.format(Date.from(recordTimestamp)).getBytes(StandardCharsets.UTF_8);
  }

  private byte[] createLocations(String handle, HandleType type) throws TransformerException {
    var document = documentBuilder.newDocument();
    var locations = document.createElement("locations");
    document.appendChild(locations);
    var firstLocation = document.createElement("location");
    firstLocation.setAttribute("id", "0");
    if (type == HandleType.MAPPING) {
      firstLocation.setAttribute("href", appProperties.getBaseUrl() + "/mapping/" + handle);
    } else if (type == HandleType.MACHINE_ANNOTATION_SERVICE) {
      firstLocation.setAttribute("href", appProperties.getBaseUrl() + "/mas/" + handle);
    } else if (type == HandleType.SOURCE_SYSTEM) {
      firstLocation.setAttribute("href", appProperties.getBaseUrl() + "/source-systems/" + handle);
    } else {
      throw new UnsupportedOperationException("Type: " + type + " is not a valid handle type");
    }
    firstLocation.setAttribute("weight", "0");
    locations.appendChild(firstLocation);
    return documentToString(document).getBytes(StandardCharsets.UTF_8);
  }

  private String documentToString(Document document) throws TransformerException {
    var tf = TransformerFactory.newInstance();
    var transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.getBuffer().toString();
  }

  private byte[] createPidReference(String pid, String pidType, String primaryNameFromPid) {
    var objectNode = mapper.createObjectNode();
    objectNode.put("id", pid);
    objectNode.put("pidType", pidType);
    objectNode.put("primaryNameFromPid", primaryNameFromPid);
    return objectNode.toString().getBytes(StandardCharsets.UTF_8);
  }


  private String generateHandle() {
    return PREFIX + newSuffix();
  }

  private String newSuffix() {
    for (int idx = 0; idx < buffer.length; ++idx) {
      if (idx == 3 || idx == 7) { //
        buffer[idx] = '-'; // Sneak a lil dash in the middle
      } else {
        buffer[idx] = symbols[random.nextInt(symbols.length)];
      }
    }
    return new String(buffer);
  }

  private byte[] decodeAdmin() {
    var admin = "0fff000000153330303a302e4e412f32302e353030302e31303235000000c8";
    byte[] adminByte = new byte[admin.length() / 2];
    for (int i = 0; i < admin.length(); i += 2) {
      adminByte[i / 2] = hexToByte(admin.substring(i, i + 2));
    }
    return adminByte;
  }

  private byte hexToByte(String hexString) {
    int firstDigit = toDigit(hexString.charAt(0));
    int secondDigit = toDigit(hexString.charAt(1));
    return (byte) ((firstDigit << 4) + secondDigit);
  }

  private int toDigit(char hexChar) {
    return Character.digit(hexChar, 16);
  }

}
