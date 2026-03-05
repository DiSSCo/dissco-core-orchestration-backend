package eu.dissco.orchestration.backend.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSetter.Value;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfiguration {

  public static final String HANDLE_PROXY = "https://hdl.handle.net/";
  public static final String DATE_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

  @Bean(name = "objectMapper")
  public ObjectMapper objectMapper() {
    var mapper = new ObjectMapper().findAndRegisterModules();
    mapper.setSerializationInclusion(Include.NON_NULL);
    return mapper;
  }

  @Bean
  @Primary
  public JsonMapper jsonMapper() {
    return JsonMapper.builder()
        .findAndAddModules()
        .defaultDateFormat(new SimpleDateFormat(DATE_STRING))
        .defaultTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
        .withConfigOverride(List.class, cfg ->
            cfg.setNullHandling(Value.forValueNulls(Nulls.AS_EMPTY)))
        .withConfigOverride(Map.class, cfg ->
            cfg.setNullHandling(Value.forValueNulls(Nulls.AS_EMPTY)))
        .withConfigOverride(Set.class, cfg ->
            cfg.setNullHandling(Value.forValueNulls(Nulls.AS_EMPTY)))
        .build();
  }

  @Bean(name = "yamlMapper")
  public YAMLMapper yamlObjectMapper() {
    return new YAMLMapper(
        YAMLMapper
            .builder()
            .findAndAddModules()
            .build());
  }

  @Bean
  public Random random() {
    return new Random();
  }

  @Bean
  public DocumentBuilder documentBuilder() throws ParserConfigurationException {
    var docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return docFactory.newDocumentBuilder();
  }

  @Bean
  public TransformerFactory transformerFactory() {
    var factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

}
