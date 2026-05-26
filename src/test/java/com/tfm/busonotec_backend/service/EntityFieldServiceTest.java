package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.support.InMemoryBusinessEntityRepository;
import com.tfm.busonotec_backend.support.InMemoryEntityFieldRepository;
import com.tfm.busonotec_backend.support.RecordingDynamicSchemaService;
import com.tfm.busonotec_backend.support.RecordingDynamicSchemaService.AddedColumn;
import com.tfm.busonotec_backend.support.RecordingDynamicSchemaService.AddedRelationshipColumn;
import com.tfm.busonotec_backend.support.RecordingDynamicSchemaService.DroppedColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tfm.busonotec_backend.support.TestFixtures.STUDENTS;
import static com.tfm.busonotec_backend.support.TestFixtures.businessEntity;
import static com.tfm.busonotec_backend.support.TestFixtures.entityField;
import static com.tfm.busonotec_backend.support.TestFixtures.entityFieldRequest;
import static com.tfm.busonotec_backend.support.TestFixtures.relationshipFieldRequest;
import static com.tfm.busonotec_backend.support.TestFixtures.studentsEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityFieldServiceTest {
  private InMemoryEntityFieldRepository fieldRepository;
  private InMemoryBusinessEntityRepository entityRepository;
  private RecordingDynamicSchemaService schemaService;
  private EntityFieldService service;
  private UUID entityId;

  @BeforeEach
  void setUp() {
    fieldRepository = new InMemoryEntityFieldRepository();
    entityRepository = new InMemoryBusinessEntityRepository();
    schemaService = new RecordingDynamicSchemaService();
    service = new EntityFieldService(fieldRepository, entityRepository, schemaService);
    entityId = UUID.randomUUID();
    entityRepository.add(studentsEntity(entityId));
  }

  @Test
  void createAddsPhysicalColumnAndPersistsFieldMetadata() {
    EntityFieldResponse response = service.create(entityFieldRequest(entityId, "score", "number"));

    assertEntityFieldResponse(response, entityId, "score", "number");
    assertThat(schemaService.addedColumns())
        .containsExactly(new AddedColumn(STUDENTS, "score", "number"));
    assertThat(fieldRepository.savedFields()).singleElement().satisfies(saved -> {
      assertThat(saved.getId()).isEqualTo(response.getId());
      assertThat(saved.getName()).isEqualTo("score");
    });
  }

  @Test
  void createPersistsValidationMetadata() {
    EntityFieldResponse response = service.create(entityFieldRequest(
        entityId,
        "nickname",
        "string",
        true,
        3,
        120,
        null,
        null,
        null,
        null
    ));

    assertThat(response.isRequired()).isTrue();
    assertThat(response.getMinLength()).isEqualTo(3);
    assertThat(response.getMaxLength()).isEqualTo(120);
    assertThat(fieldRepository.savedFields()).singleElement().satisfies(saved -> {
      assertThat(saved.isRequired()).isTrue();
      assertThat(saved.getMinLength()).isEqualTo(3);
      assertThat(saved.getMaxLength()).isEqualTo(120);
    });
  }

  @Test
  void createRelationshipAddsForeignKeyColumnAndPersistsMetadata() {
    UUID activitiesId = UUID.randomUUID();
    entityRepository.add(businessEntity(activitiesId, "Activities", "Activity records"));

    EntityFieldResponse response = service.create(relationshipFieldRequest(
        entityId,
        "activityId",
        "many_to_one",
        activitiesId
    ));

    assertEntityFieldResponse(response, entityId, "activityId", "relationship");
    assertThat(response.getRelationshipType()).isEqualTo("many_to_one");
    assertThat(response.getReferencedBusinessEntityId()).isEqualTo(activitiesId);
    assertThat(schemaService.addedRelationshipColumns())
        .containsExactly(new AddedRelationshipColumn(STUDENTS, "activityId", "Activities", "many_to_one"));
    assertThat(fieldRepository.savedFields()).singleElement().satisfies(saved -> {
      assertThat(saved.getRelationshipType()).isEqualTo("many_to_one");
      assertThat(saved.getReferencedBusinessEntityId()).isEqualTo(activitiesId);
    });
  }

  @ParameterizedTest
  @MethodSource("invalidFieldNames")
  void createRejectsInvalidNames(String fieldName) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(new EntityFieldRequest(entityId, fieldName, "string")));

    assertMessageContainsAnyOf(exception, "Field name must be provided", "Invalid field name", "reserved");
    assertCreateHadNoSideEffects();
  }

  @ParameterizedTest
  @MethodSource("invalidFieldTypes")
  void createRejectsInvalidTypes(String fieldType) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "email", fieldType)));

    assertMessageContainsAnyOf(exception, "Field type must be provided", "Unsupported field type");
    assertCreateHadNoSideEffects();
  }

  @Test
  void createRejectsValidationRulesForWrongTypes() {
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "score", "number", null, 1, null, null, null, null, null))))
        .hasMessage("Length validations are only supported for string fields");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "active", "boolean", null, null, null, BigDecimal.ZERO, null, null, null))))
        .hasMessage("Numeric validations are only supported for number fields");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "name", "string", null, null, null, null, null, LocalDate.now(), null))))
        .hasMessage("Date validations are only supported for date fields");
    assertCreateHadNoSideEffects();
  }

  @Test
  void createRejectsInvalidRelationshipDefinitions() {
    UUID activitiesId = UUID.randomUUID();
    entityRepository.add(businessEntity(activitiesId, "Activities", "Activity records"));

    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(new EntityFieldRequest(entityId, "activityId", "relationship"))))
        .hasMessage("Referenced business entity id must be provided for relationship fields");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(relationshipFieldRequest(entityId, "activityId", "many", activitiesId))))
        .hasMessage("Unsupported relationship type: many");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(new EntityFieldRequest(entityId, "name", "string", null, null, null, null, null, null, null, "many_to_one", activitiesId))))
        .hasMessage("Relationship metadata is only supported for relationship fields");
    assertCreateHadNoSideEffects();
  }

  @Test
  void createRejectsInvertedValidationRanges() {
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "name", "string", null, 5, 3, null, null, null, null))))
        .hasMessage("Minimum length must be less than or equal to maximum length");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "score", "number", null, null, null, BigDecimal.TEN, BigDecimal.ONE, null, null))))
        .hasMessage("Minimum value must be less than or equal to maximum value");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "birthDate", "date", null, null, null, null, null, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)))))
        .hasMessage("Minimum date must be less than or equal to maximum date");
    assertCreateHadNoSideEffects();
  }

  @Test
  void createRejectsNegativeLengthRanges() {
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "name", "string", null, -1, null, null, null, null, null))))
        .hasMessage("Minimum length must be greater than or equal to 0");
    assertThat(assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "name", "string", null, null, -1, null, null, null, null))))
        .hasMessage("Maximum length must be greater than or equal to 0");
    assertCreateHadNoSideEffects();
  }

  @Test
  void createRejectsMissingBusinessEntity() {
    UUID unknownId = UUID.randomUUID();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(unknownId, "email", "string")));

    assertThat(exception).hasMessage("BusinessEntity not found: " + unknownId);
    assertCreateHadNoSideEffects();
  }

  @Test
  void createRejectsNullBusinessEntityId() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(null, "email", "string")));

    assertThat(exception).hasMessage("BusinessEntity not found: null");
  }

  @Test
  void createRejectsDuplicateFieldNamesForEntity() {
    fieldRepository.markExisting(entityId, "email");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.create(entityFieldRequest(entityId, "email", "string")));

    assertThat(exception).hasMessage("Field with name already exists for entity: email");
    assertCreateHadNoSideEffects();
  }

  @Test
  void listByEntityReturnsFieldsOrderedByRepository() {
    UUID alphaId = UUID.randomUUID();
    UUID betaId = UUID.randomUUID();
    fieldRepository.add(entityField(betaId, entityId, "beta", "boolean"));
    fieldRepository.add(entityField(alphaId, entityId, "alpha", "string"));

    List<EntityFieldResponse> responses = service.listByEntity(entityId);

    assertThat(responses)
        .extracting(EntityFieldResponse::getName)
        .containsExactly("alpha", "beta");
    assertThat(responses)
        .extracting(EntityFieldResponse::getId)
        .containsExactly(alphaId, betaId);
  }

  @Test
  void deleteDropsPhysicalColumnAndDeletesFieldMetadata() {
    UUID fieldId = UUID.randomUUID();
    fieldRepository.add(entityField(fieldId, entityId, "score", "number"));

    service.delete(fieldId);

    assertThat(schemaService.droppedColumns())
        .containsExactly(new DroppedColumn(STUDENTS, "score"));
    assertThat(fieldRepository.findById(fieldId)).isEmpty();
  }

  @Test
  void deleteRejectsNullFieldId() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.delete(null));

    assertThat(exception).hasMessage("Entity field id must be provided");
    assertThat(schemaService.hasNoDroppedColumns()).isTrue();
  }

  @Test
  void deleteRejectsMissingField() {
    UUID missingId = UUID.randomUUID();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.delete(missingId));

    assertThat(exception).hasMessage("Entity field not found: " + missingId);
    assertThat(schemaService.hasNoDroppedColumns()).isTrue();
  }

  static Stream<String> invalidFieldNames() {
    return Stream.of(null, "", " ", "1email", "email-address", "field.name", "id", "ID", "a".repeat(64));
  }

  static Stream<String> invalidFieldTypes() {
    return Stream.of(null, "", " ", "json", "timestamp");
  }

  private void assertEntityFieldResponse(
      EntityFieldResponse response,
      UUID businessEntityId,
      String name,
      String type
  ) {
    assertThat(response.getId()).isNotNull();
    assertThat(response.getBusinessEntityId()).isEqualTo(businessEntityId);
    assertThat(response.getName()).isEqualTo(name);
    assertThat(response.getType()).isEqualTo(type);
  }

  private void assertCreateHadNoSideEffects() {
    assertThat(schemaService.hasNoAddedColumns()).isTrue();
    assertThat(fieldRepository.hasNoSavedFields()).isTrue();
  }

  private void assertMessageContainsAnyOf(Exception exception, String... expectedMessages) {
    assertThat(expectedMessages)
        .anySatisfy(expectedMessage -> assertThat(exception.getMessage()).contains(expectedMessage));
  }
}
