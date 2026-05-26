package com.tfm.busonotec_backend.domain;

import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainDtoTest {

  @Test
  void businessEntityAccessorsExposeState() {
    UUID id = UUID.randomUUID();
    BusinessEntity empty = new BusinessEntity();

    empty.setId(id);
    empty.setName("Students");
    empty.setDescription("Student data");

    assertBusinessEntity(empty, id, "Students", "Student data");
    assertBusinessEntity(new BusinessEntity(id, "Activities", "Activity data"), id, "Activities", "Activity data");
  }

  @Test
  void entityFieldAccessorsExposeState() {
    UUID id = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    FieldDetail detail = new FieldDetail(true, "unique", "N/A");
    EntityField empty = new EntityField();

    empty.setId(id);
    empty.setName("email");
    empty.setType("string");
    empty.setBusinessEntityId(entityId);
    empty.setDetail(detail);
    empty.setRequired(true);
    empty.setMinLength(3);
    empty.setMaxLength(120);
    empty.setMinValue(BigDecimal.ONE);
    empty.setMaxValue(BigDecimal.TEN);
    empty.setMinDate(LocalDate.of(2026, 1, 1));
    empty.setMaxDate(LocalDate.of(2026, 12, 31));
    UUID referencedEntityId = UUID.randomUUID();
    empty.setRelationshipType("many_to_one");
    empty.setReferencedBusinessEntityId(referencedEntityId);

    assertEntityField(empty, id, entityId, "email", "string");
    assertThat(empty.getDetail()).isEqualTo(detail);
    assertThat(empty.isRequired()).isTrue();
    assertThat(empty.getMinLength()).isEqualTo(3);
    assertThat(empty.getMaxLength()).isEqualTo(120);
    assertThat(empty.getMinValue()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(empty.getMaxValue()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(empty.getMinDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(empty.getMaxDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    assertThat(empty.getRelationshipType()).isEqualTo("many_to_one");
    assertThat(empty.getReferencedBusinessEntityId()).isEqualTo(referencedEntityId);

    EntityField created = new EntityField(id, "score", "number", entityId, null);

    assertEntityField(created, id, entityId, "score", "number");
    assertThat(created.getDetail()).isNull();
    assertThat(created.isRequired()).isFalse();
  }

  @Test
  void fieldDetailAccessorsExposeState() {
    FieldDetail empty = new FieldDetail();

    empty.setNullable(false);
    empty.setConstraints("required");
    empty.setDefaultValue("pending");

    assertFieldDetail(empty, false, "required", "pending");
    assertFieldDetail(new FieldDetail(true, "max:100", "0"), true, "max:100", "0");
  }

  @Test
  void requestDtosExposeConstructorValues() {
    UUID entityId = UUID.randomUUID();
    BusinessEntityRequest entityRequest = new BusinessEntityRequest("Students", "Student data");
    EntityFieldRequest fieldRequest = new EntityFieldRequest(
        entityId,
        "email",
        "string",
        true,
        3,
        120,
        null,
        null,
        null,
        null
    );
    UUID referencedEntityId = UUID.randomUUID();
    EntityFieldRequest relationshipRequest = new EntityFieldRequest(
        entityId,
        "activityId",
        "relationship",
        true,
        null,
        null,
        null,
        null,
        null,
        null,
        "many_to_one",
        referencedEntityId
    );

    assertThat(entityRequest.getName()).isEqualTo("Students");
    assertThat(entityRequest.getDescription()).isEqualTo("Student data");
    assertThat(fieldRequest.getBusinessEntityId()).isEqualTo(entityId);
    assertThat(fieldRequest.getName()).isEqualTo("email");
    assertThat(fieldRequest.getType()).isEqualTo("string");
    assertThat(fieldRequest.getRequired()).isTrue();
    assertThat(fieldRequest.getMinLength()).isEqualTo(3);
    assertThat(fieldRequest.getMaxLength()).isEqualTo(120);
    assertThat(relationshipRequest.getRelationshipType()).isEqualTo("many_to_one");
    assertThat(relationshipRequest.getReferencedBusinessEntityId()).isEqualTo(referencedEntityId);
  }

  @Test
  void responseDtosExposeConstructorAndSetterValues() {
    UUID entityId = UUID.randomUUID();
    UUID fieldId = UUID.randomUUID();
    BusinessEntityResponse entityResponse = new BusinessEntityResponse();
    EntityFieldResponse fieldResponse = new EntityFieldResponse();

    entityResponse.setId(entityId);
    entityResponse.setName("Students");
    entityResponse.setDescription("Student data");
    fieldResponse.setId(fieldId);
    fieldResponse.setBusinessEntityId(entityId);
    fieldResponse.setName("email");
    fieldResponse.setType("string");
    fieldResponse.setRequired(true);
    fieldResponse.setMinLength(3);
    fieldResponse.setMaxLength(120);
    fieldResponse.setMinValue(BigDecimal.ONE);
    fieldResponse.setMaxValue(BigDecimal.TEN);
    fieldResponse.setMinDate(LocalDate.of(2026, 1, 1));
    fieldResponse.setMaxDate(LocalDate.of(2026, 12, 31));
    UUID referencedEntityId = UUID.randomUUID();
    fieldResponse.setRelationshipType("one_to_one");
    fieldResponse.setReferencedBusinessEntityId(referencedEntityId);

    assertBusinessEntityResponse(entityResponse, entityId, "Students", "Student data");
    assertBusinessEntityResponse(
        new BusinessEntityResponse(entityId, "Activities", "Activity data"),
        entityId,
        "Activities",
        "Activity data"
    );
    assertEntityFieldResponse(fieldResponse, fieldId, entityId, "email", "string");
    assertThat(fieldResponse.isRequired()).isTrue();
    assertThat(fieldResponse.getMinLength()).isEqualTo(3);
    assertThat(fieldResponse.getMaxLength()).isEqualTo(120);
    assertThat(fieldResponse.getMinValue()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(fieldResponse.getMaxValue()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(fieldResponse.getMinDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(fieldResponse.getMaxDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    assertThat(fieldResponse.getRelationshipType()).isEqualTo("one_to_one");
    assertThat(fieldResponse.getReferencedBusinessEntityId()).isEqualTo(referencedEntityId);
    assertEntityFieldResponse(
        new EntityFieldResponse(fieldId, entityId, "score", "number"),
        fieldId,
        entityId,
        "score",
        "number"
    );
  }

  private void assertBusinessEntity(BusinessEntity entity, UUID id, String name, String description) {
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getName()).isEqualTo(name);
    assertThat(entity.getDescription()).isEqualTo(description);
  }

  private void assertEntityField(EntityField field, UUID id, UUID entityId, String name, String type) {
    assertThat(field.getId()).isEqualTo(id);
    assertThat(field.getBusinessEntityId()).isEqualTo(entityId);
    assertThat(field.getName()).isEqualTo(name);
    assertThat(field.getType()).isEqualTo(type);
  }

  private void assertFieldDetail(FieldDetail detail, Boolean nullable, String constraints, String defaultValue) {
    assertThat(detail.getNullable()).isEqualTo(nullable);
    assertThat(detail.getConstraints()).isEqualTo(constraints);
    assertThat(detail.getDefaultValue()).isEqualTo(defaultValue);
  }

  private void assertBusinessEntityResponse(
      BusinessEntityResponse response,
      UUID id,
      String name,
      String description
  ) {
    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getName()).isEqualTo(name);
    assertThat(response.getDescription()).isEqualTo(description);
  }

  private void assertEntityFieldResponse(
      EntityFieldResponse response,
      UUID id,
      UUID entityId,
      String name,
      String type
  ) {
    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getBusinessEntityId()).isEqualTo(entityId);
    assertThat(response.getName()).isEqualTo(name);
    assertThat(response.getType()).isEqualTo(type);
  }
}
