package eu.dissco.orchestration.backend.repository;

import eu.dissco.orchestration.backend.domain.TranslatorJobRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static eu.dissco.orchestration.backend.database.jooq.Tables.TRANSLATOR_JOB_RECORD;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;

@Repository
@RequiredArgsConstructor
public class TranslatorJobRecordRepository {

	private final DSLContext context;

	private final JsonMapper mapper;

	public List<TranslatorJobRecord> getJobRecords(String id, int pageNum, int pageSize) {
		return context.selectFrom(TRANSLATOR_JOB_RECORD)
			.where(TRANSLATOR_JOB_RECORD.SOURCE_SYSTEM_ID.eq(id))
			.orderBy(TRANSLATOR_JOB_RECORD.TIME_STARTED.desc())
			.offset(getOffset(pageNum, pageSize))
			.limit(pageSize + 1)
			.fetch()
			.stream()
			.map(this::mapTranslatorJobRecord)
			.toList();
	}

	private TranslatorJobRecord mapTranslatorJobRecord(Record jobRecord) {
		var report = jobRecord.get(TRANSLATOR_JOB_RECORD.REPORT) != null
				? mapper.readTree(jobRecord.get(TRANSLATOR_JOB_RECORD.REPORT).data()) : null;
		return new TranslatorJobRecord(jobRecord.get(TRANSLATOR_JOB_RECORD.JOB_ID),
				jobRecord.get(TRANSLATOR_JOB_RECORD.TIME_STARTED), jobRecord.get(TRANSLATOR_JOB_RECORD.TIME_COMPLETED),
				jobRecord.get(TRANSLATOR_JOB_RECORD.JOB_STATE), report);
	}

}
