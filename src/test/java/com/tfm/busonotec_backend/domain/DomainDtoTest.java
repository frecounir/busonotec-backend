package com.tfm.busonotec_backend.domain;

import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import org.junit.jupiter.api.Test;

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

    assertEntityField(empty, id, entityId, "email", "string");
    assertThat(empty.getDetail()).isEqualTo(detail);

    EntityField created = new EntityField(id, "score", "number", entityId, null);

    assertEntityField(created, id, entityId, "score", "number");
    assertThat(created.getDetail()).isNull();
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
    EntityFieldRequest fieldRequest = new EntityFieldRequest(entityId, "email", "string");

    assertThat(entityRequest.getName()).isEqualTo("Students");
    assertThat(entityRequest.getDescription()).isEqualTo("Student data");
    assertThat(fieldRequest.getBusinessEntityId()).isEqualTo(entityId);
    assertThat(fieldRequest.getName()).isEqualTo("email");
    assertThat(fieldRequest.getType()).isEqualTo("string");
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

    assertBusinessEntityResponse(entityResponse, entityId, "Students", "Student data");
    assertBusinessEntityResponse(
        new BusinessEntityResponse(entityId, "Activities", "Activity data"),
        entityId,
        "Activities",
        "Activity data"
    );
    assertEntityFieldResponse(fieldResponse, fieldId, entityId, "email", "string");
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
