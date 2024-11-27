package eu.dissco.orchestration.backend.domain.openapi.datamapping;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record DataMappingResponseSchema(
    DataMappingResponseData data
){

}
