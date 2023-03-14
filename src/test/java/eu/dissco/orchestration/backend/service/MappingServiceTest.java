package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_DESCRIPTION;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecordResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.repository.MappingRepository;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MappingServiceTest {

  private MappingService service;
  @Mock
  private HandleService handleService;
  @Mock
  private MappingRepository repository;

  private MockedStatic<Instant> mockedStatic;

  @BeforeEach
  void setup(){
    service = new MappingService(handleService, repository, MAPPER);
    initTime();
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
  }

  @Test
  void testCreateMapping() throws  Exception{
    // Given
    var mapping = givenMapping();
    var expected = givenMappingRecord(HANDLE, 1);
    given(handleService.createNewHandle(HandleType.MAPPING)).willReturn(HANDLE);

    // When
    var result = service.createMapping(mapping, OBJECT_CREATOR);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testUpdateMapping(){
    // Given
    var prevMapping = new Mapping("old name", OBJECT_DESCRIPTION, MAPPER.createObjectNode());
    var prevRecord = new MappingRecord(HANDLE, 1,CREATED, OBJECT_CREATOR, prevMapping);
    var mapping = givenMapping();
    var expected = givenMappingRecord(HANDLE, 2);

    given(repository.getMapping(HANDLE)).willReturn(prevRecord);

    // When
    var result = service.updateMapping(HANDLE, mapping, OBJECT_CREATOR);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testUpdateMappingNoChanges(){
    // Given
    var prevMapping = givenMappingRecord(HANDLE, 1);
    var mapping = givenMapping();

    given(repository.getMapping(HANDLE)).willReturn(prevMapping);

    // When
    var result = service.updateMapping(HANDLE, mapping, OBJECT_CREATOR);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testGetMappingsById(){
    // Given
    var expected = givenMappingRecord(HANDLE, 1);
    given(repository.getMapping(HANDLE)).willReturn(expected);

    // When
    var result = service.getMappingById(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMappings(){
    // Given
    int pageSize = 10;
    int pageNum = 1;
    List<MappingRecord> mappingRecords = Collections.nCopies(pageSize+1, givenMappingRecord(HANDLE, 1));
    given(repository.getMappings(pageNum, pageSize+1)).willReturn(mappingRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, SANDBOX_URI);
    var expected = givenMappingRecordResponse(mappingRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getMappings(pageNum, pageSize, SANDBOX_URI);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMappingsLastPage(){
    // Given
    int pageSize = 10;
    int pageNum = 2;
    List<MappingRecord> mappingRecords = Collections.nCopies(pageSize, givenMappingRecord(HANDLE, 1));
    given(repository.getMappings(pageNum, pageSize+1)).willReturn(mappingRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, SANDBOX_URI);
    var expected = givenMappingRecordResponse(mappingRecords, linksNode);

    // When
    var result = service.getMappings(pageNum, pageSize, SANDBOX_URI);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  private void initTime() {
    Clock clock = Clock.fixed(CREATED, ZoneOffset.UTC);
    Instant instant = Instant.now(clock);
    mockedStatic = mockStatic(Instant.class);
    mockedStatic.when(Instant::now).thenReturn(instant);
    mockedStatic.when(() -> Instant.from(any())).thenReturn(instant);
  }

}
