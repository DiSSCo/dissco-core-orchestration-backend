package eu.dissco.orchestration.backend.component;

import eu.dissco.orchestration.backend.domain.ExportType;
import org.springframework.core.convert.converter.Converter;

public class StringToExportTypeConverter implements Converter<String, ExportType> {


  @Override
  public ExportType convert(String exportTypeName) {
    return ExportType.fromName(exportTypeName);
  }
}
