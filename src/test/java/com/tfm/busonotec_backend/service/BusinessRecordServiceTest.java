package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.support.InMemoryBusinessEntityRepository;
import com.tfm.busonotec_backend.support.InMemoryBusinessRecordRepository;
import com.tfm.busonotec_backend.support.InMemoryEntityFieldRepository;
import com.tfm.busonotec_backend.support.RecordingDynamicSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tfm.busonotec_backend.support.TestFixtures.STUDENTS;
import static com.tfm.busonotec_backend.support.TestFixtures.entityField;
import static com.tfm.busonotec_backend.support.TestFixtures.studentsEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessRecordServiceTest {
  private InMemoryBusinessEntityRepository entityRepository;
  private InMemoryEntityFieldRepository fieldRepository;
  private InMemoryBusinessRecordRepository recordRepository;
  private RecordingDynamicSchemaService schemaService;
  private BusinessRecordService service;
  private UUID entityId;

  @BeforeEach
  void setUp() {
    entityRepository = new InMemoryBusinessEntityRepository();
    fieldRepository = new InMemoryEntityFieldRepository();
    recordRepository = new InMemoryBusinessRecordRepository();
    schemaService = new RecordingDynamicSchemaService();
    service = new BusinessRecordService(entityRepository, fieldRepository, recordRepository, schemaService);
    entityId = UUID.randomUUID();
  }

  @Test
  void listByBusinessEntityReturnsRecordsFromPhysicalTable() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);
    Map<String, Object> record = Map.of("id", UUID.randomUUID(), "score", 95);
    recordRepository.add(record);

    List<Map<String, Object>> records = service.listByBusinessEntity(entityId);

    assertThat(records).containsExactly(record);
    assertThat(recordRepository.queriedEntityNames()).containsExactly(STUDENTS);
  }

  @Test
  void listByBusinessEntityRejectsMissingInput() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.listByBusinessEntity(null));

    assertThat(exception).hasMessage("Business entity id must be provided");
    assertThat(recordRepository.hasNoQueries()).isTrue();
  }

  @Test
  void listByBusinessEntityRejectsUnknownBusinessEntity() {
    UUID unknownId = UUID.randomUUID();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.listByBusinessEntity(unknownId));

    assertThat(exception).hasMessage("Business entity not found: " + unknownId);
    assertThat(recordRepository.hasNoQueries()).isTrue();
  }

  @Test
  void listByBusinessEntityRejectsMissingPhysicalTable() {
    entityRepository.add(studentsEntity(entityId));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.listByBusinessEntity(entityId));

    assertThat(exception).hasMessage("Physical table does not exist for entity: Students");
    assertThat(recordRepository.hasNoQueries()).isTrue();
  }

  @Test
  void createStoresRecordWithGeneratedIdAndMetadataFields() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "score", "number"));

    Map<String, Object> record = service.create(entityId, Map.of("score", 95));

    assertThat(record.get("id")).isInstanceOf(UUID.class);
    assertThat(record.get("score")).isEqualTo(95);
    assertThat(recordRepository.createdEntityNames()).containsExactly(STUDENTS);
  }

  @Test
  void createNormalizesFieldNamesCaseInsensitively() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "Score", "number"));

    Map<String, Object> record = service.create(entityId, Map.of("SCORE", 95));

    assertThat(record).containsEntry("score", 95);
  }

  @Test
  void createAllowsEmptyRecordBodyWhenEntityHasNoDynamicFields() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);

    Map<String, Object> record = service.create(entityId, Map.of());

    assertThat(record).containsOnlyKeys("id");
  }

  @Test
  void createConvertsDateFieldStringsToLocalDate() {
    prepareStudentsEntity();
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "enrollmentDate", "date"));

    Map<String, Object> record = service.create(entityId, Map.of("enrollmentDate", "2026-05-19"));

    assertThat(record).containsEntry("enrollmentdate", LocalDate.of(2026, 5, 19));
  }

  @Test
  void createRejectsMissingRecordBody() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(entityId, null));

    assertThat(exception).hasMessage("Record body must be provided");
    assertThat(recordRepository.createdEntityNames()).isEmpty();
  }

  @Test
  void createRejectsUnknownRecordField() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "score", "number"));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(entityId, Map.of("nickname", "Ana")));

    assertThat(exception).hasMessage("Field is not defined for entity: nickname");
    assertThat(recordRepository.createdEntityNames()).isEmpty();
  }

  @Test
  void createRejectsInvalidDateFieldValues() {
    prepareStudentsEntity();
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "enrollmentDate", "date"));

    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityId, Map.of("enrollmentDate", "19-05-2026"))))
        .hasMessage("Invalid date value for field enrollmentdate. Expected format: yyyy-MM-dd");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityId, Map.of("enrollmentDate", 20260519))))
        .hasMessage("Invalid date value for field enrollmentdate. Expected format: yyyy-MM-dd");
    assertThat(recordRepository.createdEntityNames()).isEmpty();
  }

  @Test
  void createRejectsInvalidReservedOrDuplicateRecordFields() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "score", "number"));
    Map<String, Object> duplicateFields = new LinkedHashMap<>();
    duplicateFields.put("score", 95);
    duplicateFields.put("SCORE", 90);

    assertThat(assertThrows(IllegalArgumentException.class, () -> service.create(entityId, Map.of("id", "custom"))))
        .hasMessage("Record field name 'id' is reserved");
    assertThat(assertThrows(IllegalArgumentException.class, () -> service.create(entityId, Map.of("score-value", 95))))
        .hasMessage("Invalid record field name: score-value");
    assertThat(assertThrows(IllegalArgumentException.class, () -> service.create(entityId, duplicateFields)))
        .hasMessage("Duplicate field in record: SCORE");
  }

  @Test
  void createRejectsMissingRecordFieldNames() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);
    Map<String, Object> nullFieldName = new LinkedHashMap<>();
    nullFieldName.put(null, 95);

    assertThat(assertThrows(IllegalArgumentException.class, () -> service.create(entityId, nullFieldName)))
        .hasMessage("Record field name must be provided");
    assertThat(assertThrows(IllegalArgumentException.class, () -> service.create(entityId, Map.of("", 95))))
        .hasMessage("Record field name must be provided");
  }

  @Test
  void updateChangesAllowedFieldsAndReturnsUpdatedRecord() {
    prepareStudentsEntityWithScoreField();
    UUID recordId = UUID.randomUUID();
    recordRepository.add(new LinkedHashMap<>(Map.of("id", recordId, "score", 95)));

    Map<String, Object> record = service.update(entityId, recordId, Map.of("SCORE", 100));

    assertThat(record).containsEntry("id", recordId)
        .containsEntry("score", 100);
    assertThat(recordRepository.updatedEntityNames()).containsExactly(STUDENTS);
  }

  @Test
  void updateConvertsDateFieldStringsToLocalDate() {
    prepareStudentsEntity();
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "enrollmentDate", "date"));
    UUID recordId = UUID.randomUUID();
    recordRepository.add(new LinkedHashMap<>(Map.of("id", recordId, "enrollmentdate", LocalDate.of(2026, 5, 18))));

    Map<String, Object> record = service.update(entityId, recordId, Map.of("enrollmentDate", "2026-05-19"));

    assertThat(record).containsEntry("enrollmentdate", LocalDate.of(2026, 5, 19));
  }

  @Test
  void updateRejectsMissingRecordIdOrEmptyBody() {
    prepareStudentsEntityWithScoreField();
    UUID recordId = UUID.randomUUID();

    assertThat(assertThrows(IllegalArgumentException.class, () -> service.update(entityId, null, Map.of("score", 100))))
        .hasMessage("Record id must be provided");
    assertThat(assertThrows(IllegalArgumentException.class, () -> service.update(entityId, recordId, Map.of())))
        .hasMessage("Record body must include at least one field");
    assertThat(recordRepository.updatedEntityNames()).isEmpty();
  }

  @Test
  void updateRejectsUnknownRecordField() {
    prepareStudentsEntityWithScoreField();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.update(entityId, UUID.randomUUID(), Map.of("nickname", "Ana")));

    assertThat(exception).hasMessage("Field is not defined for entity: nickname");
    assertThat(recordRepository.updatedEntityNames()).isEmpty();
  }

  @Test
  void updateRejectsMissingRecord() {
    prepareStudentsEntityWithScoreField();
    UUID missingRecordId = UUID.randomUUID();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.update(entityId, missingRecordId, Map.of("score", 100)));

    assertThat(exception).hasMessage("Record not found: " + missingRecordId);
    assertThat(recordRepository.updatedEntityNames()).containsExactly(STUDENTS);
  }

  @Test
  void deleteRemovesRecordFromPhysicalTable() {
    prepareStudentsEntity();
    UUID recordId = UUID.randomUUID();
    recordRepository.add(new LinkedHashMap<>(Map.of("id", recordId, "score", 95)));

    service.delete(entityId, recordId);

    assertThat(recordRepository.deletedEntityNames()).containsExactly(STUDENTS);
    assertThat(recordRepository.findAllByEntityName(STUDENTS)).isEmpty();
  }

  @Test
  void deleteRejectsMissingRecordIdOrMissingRecord() {
    prepareStudentsEntity();
    UUID missingRecordId = UUID.randomUUID();

    assertThat(assertThrows(IllegalArgumentException.class, () -> service.delete(entityId, null)))
        .hasMessage("Record id must be provided");
    assertThat(assertThrows(IllegalArgumentException.class, () -> service.delete(entityId, missingRecordId)))
        .hasMessage("Record not found: " + missingRecordId);
    assertThat(recordRepository.deletedEntityNames()).containsExactly(STUDENTS);
  }

  private void prepareStudentsEntityWithScoreField() {
    prepareStudentsEntity();
    fieldRepository.add(entityField(UUID.randomUUID(), entityId, "score", "number"));
  }

  private void prepareStudentsEntity() {
    entityRepository.add(studentsEntity(entityId));
    schemaService.markEntityAsExisting(STUDENTS);
  }
}
