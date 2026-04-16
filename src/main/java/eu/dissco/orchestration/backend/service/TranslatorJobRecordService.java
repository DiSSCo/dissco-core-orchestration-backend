package eu.dissco.orchestration.backend.service;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.repository.TranslatorJobRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import static eu.dissco.orchestration.backend.domain.ObjectType.TRANSLATOR_JOB_RECORD;

@Service
@RequiredArgsConstructor
public class TranslatorJobRecordService {

  private final TranslatorJobRecordRepository repository;
  private final JsonMapper mapper;

  public JsonApiListWrapper retrieveJobRecords(String id, int pageNum, int pageSize, String path) {
    var translatorJobRecords = repository.getJobRecords(id, pageNum, pageSize);
    boolean hasNext = translatorJobRecords.size() > pageSize;
    translatorJobRecords = hasNext ? translatorJobRecords.subList(0, pageSize) : translatorJobRecords;
    var jsonData = translatorJobRecords.stream().map(
        jobRecord -> new JsonApiData(jobRecord.jobId().toString(), TRANSLATOR_JOB_RECORD,
            mapper.valueToTree(jobRecord))).toList();
    return new JsonApiListWrapper(jsonData, new JsonApiLinks(pageSize, pageNum, hasNext, path));
  }
}
