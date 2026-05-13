package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.support.InMemoryBusinessEntityRepository;
import com.tfm.busonotec_backend.support.InMemoryEntityFieldRepository;
import com.tfm.busonotec_backend.support.RecordingDynamicSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tfm.busonotec_backend.support.TestFixtures.STUDENTS;
import static com.tfm.busonotec_backend.support.TestFixtures.STUDENT_DESCRIPTION;
import static com.tfm.busonotec_backend.support.TestFixtures.businessEntity;
import static com.tfm.busonotec_backend.support.TestFixtures.businessEntityRequest;
import static com.tfm.busonotec_backend.support.TestFixtures.entityField;
import static com.tfm.busonotec_backend.support.TestFixtures.studentsEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessEntityServiceTest {
  private static final String CREATE_STUDENTS_TABLE =
      "CREATE TABLE IF NOT EXISTS \"students\" (id UUID PRIMARY KEY)";

  private InMemoryBusinessEntityRepository repository;
  private InMemoryEntityFieldRepository fieldRepository;
  private RecordingDynamicSchemaService schemaService;
  private BusinessEntityService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryBusinessEntityRepository();
    fieldRepository = new InMemoryEntityFieldRepository();
    schemaService = new RecordingDynamicSchemaService();
    service = new BusinessEntityService(repository, fieldRepository, schemaService);
  }

  @Test
  void createPersistsMetadataAndCreatesPhysicalTable() {
    BusinessEntityResponse response = service.create(businessEntityRequest(STUDENTS, STUDENT_DESCRIPTION));

    assertBusinessEntityResponse(response, STUDENTS, STUDENT_DESCRIPTION);
    assertThat(repository.savedEntities()).singleElement().satisfies(saved -> {
      assertThat(saved.getId()).isEqualTo(response.getId());
      assertThat(saved.getName()).isEqualTo(STUDENTS);
    });
    assertThat(schemaService.statementFor(STUDENTS)).contains(CREATE_STUDENTS_TABLE);
  }

  @ParameterizedTest
  @MethodSource("invalidEntityNames")
  void createRejectsInvalidNames(String name) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(new BusinessEntityRequest(name, "Invalid")));

    assertMessageContainsAnyOf(exception, "Entity name must be provided", "Invalid entity name");
    assertCreateHadNoSideEffects();
  }

  @Test
  void createRejectsDuplicateNames() {
    repository.add(studentsEntity(UUID.randomUUID()));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(businessEntityRequest(STUDENTS, "Duplicate")));

    assertThat(exception).hasMessage("Entity with name already exists: Students");
    assertCreateHadNoSideEffects();
  }

  @Test
  void listReturnsBusinessEntitiesOrderedByRepository() {
    repository.add(businessEntity(UUID.randomUUID(), "Beta", "Second"));
    repository.add(businessEntity(UUID.randomUUID(), "Alpha", "First"));

    List<BusinessEntityResponse> responses = service.list();

    assertThat(responses)
        .extracting(BusinessEntityResponse::getName)
        .containsExactly("Alpha", "Beta");
  }

  @Test
  void findByIdReturnsEntity() {
    UUID id = UUID.randomUUID();
    repository.add(studentsEntity(id));

    BusinessEntityResponse response = service.findById(id);

    assertThat(response.getId()).isEqualTo(id);
    assertBusinessEntityResponse(response, STUDENTS, STUDENT_DESCRIPTION);
  }

  @Test
  void findByIdRejectsMissingInput() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.findById(null));

    assertThat(exception).hasMessage("Business entity id must be provided");
  }

  @Test
  void findByIdRejectsUnknownEntity() {
    UUID id = UUID.randomUUID();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.findById(id));

    assertThat(exception).hasMessage("Business entity not found: " + id);
  }

  @Test
  void deleteRemovesMetadataFieldsAndPhysicalTable() {
    UUID id = UUID.randomUUID();
    repository.add(studentsEntity(id));
    fieldRepository.add(entityField(UUID.randomUUID(), id, "score", "number"));
    schemaService.markEntityAsExisting(STUDENTS);

    service.delete(id);

    assertThat(repository.findById(id)).isEmpty();
    assertThat(repository.existsByName(STUDENTS)).isFalse();
    assertThat(fieldRepository.findByBusinessEntityId(id)).isEmpty();
    assertThat(schemaService.droppedEntities()).containsExactly(STUDENTS);
  }

  @Test
  void deleteRejectsMissingInput() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.delete(null));

    assertThat(exception).hasMessage("Business entity id must be provided");
  }

  @Test
  void deleteRejectsUnknownEntity() {
    UUID id = UUID.randomUUID();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.delete(id));

    assertThat(exception).hasMessage("Business entity not found: " + id);
    assertThat(schemaService.droppedEntities()).isEmpty();
  }

  static Stream<String> invalidEntityNames() {
    return Stream.of(null, "", " ", "1student", "student-name", "student.name", "a".repeat(64));
  }

  private void assertBusinessEntityResponse(BusinessEntityResponse response, String name, String description) {
    assertThat(response.getId()).isNotNull();
    assertThat(response.getName()).isEqualTo(name);
    assertThat(response.getDescription()).isEqualTo(description);
  }

  private void assertCreateHadNoSideEffects() {
    assertThat(repository.hasNoSavedEntities()).isTrue();
    assertThat(schemaService.hasNoStatements()).isTrue();
  }

  private void assertMessageContainsAnyOf(Exception exception, String... expectedMessages) {
    assertThat(expectedMessages)
        .anySatisfy(expectedMessage -> assertThat(exception.getMessage()).contains(expectedMessage));
  }
}
