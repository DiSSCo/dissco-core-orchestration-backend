package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecordResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
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
class SourceSystemServiceTest {
  private SourceSystemService service;
  @Mock
  private HandleService handleService;
  @Mock
  private SourceSystemRepository repository;

  private MockedStatic<Instant> mockedStatic;

  @BeforeEach
  void setup(){
    service = new SourceSystemService(repository, handleService, MAPPER);
    initTime();
  }
  @AfterEach
  void destroy() {
    mockedStatic.close();
  }

  @Test
  void testCreateSourceSystem() throws Exception{
    // Given
    var expected = givenSourceSystemRecord();
    var ss = givenSourceSystem();
    given(handleService.createNewHandle(HandleType.SOURCE_SYSTEM)).willReturn(HANDLE);

    // When
    var result = service.createSourceSystem(ss);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testUpdateSourceSystem(){
    var expected = givenSourceSystemRecord();
    var ss = givenSourceSystem();

    // When
    var result = service.updateSourceSystem(HANDLE, ss);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystemById(){
    // Given
    var expected = givenSourceSystemRecord();
    given(repository.getSourceSystemById(HANDLE)).willReturn(expected);

    // When
    var result = service.getSourceSystemById(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getSourceSystemRecords(){
    // Given
    int pageNum = 1;
    int pageSize = 10;
    String path = SANDBOX_URI;
    List<SourceSystemRecord> ssRecords = Collections.nCopies(pageSize+1, givenSourceSystemRecord());
    given(repository.getSourceSystems(pageNum, pageSize+1)).willReturn(ssRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, path);
    var expected = givenSourceSystemRecordResponse(ssRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getSourceSystemRecords(pageNum, pageSize, path);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getSourceSystemRecordsLastPage(){
    // Given
    int pageNum = 2;
    int pageSize = 10;
    String path = SANDBOX_URI;
    List<SourceSystemRecord> ssRecords = Collections.nCopies(pageSize, givenSourceSystemRecord());
    given(repository.getSourceSystems(pageNum, pageSize+1)).willReturn(ssRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, path);
    var expected = givenSourceSystemRecordResponse(ssRecords, linksNode);

    // When
    var result = service.getSourceSystemRecords(pageNum, pageSize, path);

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
