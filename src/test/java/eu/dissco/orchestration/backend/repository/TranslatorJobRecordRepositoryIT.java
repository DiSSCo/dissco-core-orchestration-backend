package eu.dissco.orchestration.backend.repository;

import eu.dissco.orchestration.backend.database.jooq.enums.JobState;
import eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType;
import eu.dissco.orchestration.backend.domain.TranslatorJobRecord;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import org.jooq.JSONB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static eu.dissco.orchestration.backend.database.jooq.Tables.SOURCE_SYSTEM;
import static eu.dissco.orchestration.backend.database.jooq.Tables.TRANSLATOR_JOB_RECORD;
import static eu.dissco.orchestration.backend.testutils.TestUtils.*;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;
import static org.assertj.core.api.Assertions.assertThat;

class TranslatorJobRecordRepositoryIT extends BaseRepositoryIT {

	private TranslatorJobRecordRepository repository;

	@BeforeEach
	void setup() {
		repository = new TranslatorJobRecordRepository(context, MAPPER);
	}

	@AfterEach
	void destroy() {
		context.truncate(TRANSLATOR_JOB_RECORD).execute();
	}

	@Test
	void testGetTranslatorJobRecords() {
		// Given
		int pageNum = 1;
		int pageSize = 10;
		List<TranslatorJobRecord> jobRecords = generateJobRecords();
		postJobRecords(jobRecords);

		// When
		var result = repository.getJobRecords(BARE_HANDLE, pageNum, pageSize);

		assertThat(result).isEqualTo(jobRecords);
	}

	private void postJobRecords(List<TranslatorJobRecord> jobRecords) {
		var anotherSourceSystemHandle = PREFIX + "GW0-POP-XSL";
		insertSourceSystem(List.of(givenSourceSystem(), givenSourceSystem(anotherSourceSystemHandle, 1,
				SourceSystem.OdsTranslatorType.DWCA, "anotherSourceSystem", "anotherendpoint")));
		insertJobRecords(jobRecords, BARE_HANDLE);
	}

	private void insertJobRecords(List<TranslatorJobRecord> jobRecords, String sourceSystem) {
		jobRecords.forEach(jobRecord -> context.insertInto(TRANSLATOR_JOB_RECORD)
			.set(TRANSLATOR_JOB_RECORD.JOB_ID, jobRecord.jobId())
			.set(TRANSLATOR_JOB_RECORD.SOURCE_SYSTEM_ID, sourceSystem)
			.set(TRANSLATOR_JOB_RECORD.TIME_STARTED, jobRecord.startTime())
			.set(TRANSLATOR_JOB_RECORD.TIME_COMPLETED, jobRecord.endTime())
			.set(TRANSLATOR_JOB_RECORD.JOB_STATE, jobRecord.jobState())
			.set(TRANSLATOR_JOB_RECORD.REPORT,
					(jobRecord.report() != null ? JSONB.valueOf(MAPPER.writeValueAsString(jobRecord.report())) : null))
			.execute());
	}

	private void insertSourceSystem(List<SourceSystem> sourceSystems) {
		sourceSystems.forEach(sourceSystem -> context.insertInto(SOURCE_SYSTEM)
			.set(SOURCE_SYSTEM.ID, removeProxy(sourceSystem.getId()))
			.set(SOURCE_SYSTEM.VERSION, sourceSystem.getSchemaVersion())
			.set(SOURCE_SYSTEM.NAME, sourceSystem.getSchemaName())
			.set(SOURCE_SYSTEM.ENDPOINT, sourceSystem.getSchemaUrl().toString())
			.set(SOURCE_SYSTEM.FILTERS, sourceSystem.getOdsFilters().toArray(new String[0]))
			.set(SOURCE_SYSTEM.CREATOR, sourceSystem.getSchemaCreator().getId())
			.set(SOURCE_SYSTEM.CREATED, sourceSystem.getSchemaDateCreated().toInstant())
			.set(SOURCE_SYSTEM.MODIFIED, sourceSystem.getSchemaDateModified().toInstant())
			.set(SOURCE_SYSTEM.MAPPING_ID, sourceSystem.getOdsDataMappingID())
			.set(SOURCE_SYSTEM.TRANSLATOR_TYPE, TranslatorType.valueOf(sourceSystem.getOdsTranslatorType().value()))
			.set(SOURCE_SYSTEM.DWC_DP_LINK, DWC_DP_S3_URI)
			.set(SOURCE_SYSTEM.DATA, JSONB.valueOf(MAPPER.writeValueAsString(sourceSystem)))
			.execute());
	}

	private List<TranslatorJobRecord> generateJobRecords() {
		var list = new ArrayList<TranslatorJobRecord>();
		for (int i = 0; i < 10; i++) {
			list.add(new TranslatorJobRecord(UUID.randomUUID(), CREATED, Instant.parse("2024-11-01T10:05:24.00Z"),
					JobState.COMPLETED, getJobRecordReport()));
		}
		return list;
	}

}
