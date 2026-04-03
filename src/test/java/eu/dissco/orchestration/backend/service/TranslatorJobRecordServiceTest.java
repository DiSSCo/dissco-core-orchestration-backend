package eu.dissco.orchestration.backend.service;

import eu.dissco.orchestration.backend.domain.TranslatorJobRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.repository.TranslatorJobRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static eu.dissco.orchestration.backend.testutils.TestUtils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TranslatorJobRecordServiceTest {

	@Mock
	private TranslatorJobRecordRepository repository;

	private TranslatorJobRecordService service;

	@BeforeEach
	void setupAll() {
		service = new TranslatorJobRecordService(repository, MAPPER);
	}

	@Test
	void getTranslatorJobRecord() {
		// Given
		int pageNum = 1;
		int pageSize = 10;
		String path = JOB_RECORD_PATH;
		List<TranslatorJobRecord> translatorJobRecords = Collections.nCopies(pageSize + 1, givenTranslatorJobRecord());
		given(repository.getJobRecords(BARE_HANDLE, pageNum, pageSize)).willReturn(translatorJobRecords);
		var linksNode = new JsonApiLinks(pageSize, pageNum, true, path);
		var expected = givenTranslatorJobRecordResponse(translatorJobRecords.subList(0, pageSize), linksNode);

		// When
		var result = service.retrieveJobRecords(BARE_HANDLE, pageNum, pageSize, path);

		// Then
		assertThat(result).isEqualTo(expected);
	}

}
