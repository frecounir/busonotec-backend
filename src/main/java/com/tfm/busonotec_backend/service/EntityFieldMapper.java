package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntityFieldMapper {
  public EntityField toDomain(UUID id, EntityFieldRequest request, String normalizedType, String normalizedRelationshipType) {
    return new EntityField(
        id,
        request.getName(),
        normalizedType,
        request.getBusinessEntityId(),
        null,
        Boolean.TRUE.equals(request.getRequired()),
        request.getMinLength(),
        request.getMaxLength(),
        request.getMinValue(),
        request.getMaxValue(),
        request.getMinDate(),
        request.getMaxDate(),
        normalizedRelationshipType,
        request.getReferencedBusinessEntityId()
    );
  }

  public EntityFieldResponse toResponse(EntityField field) {
    return new EntityFieldResponse(
        field.getId(),
        field.getBusinessEntityId(),
        field.getName(),
        field.getType(),
        field.isRequired(),
        field.getMinLength(),
        field.getMaxLength(),
        field.getMinValue(),
        field.getMaxValue(),
        field.getMinDate(),
        field.getMaxDate(),
        field.getRelationshipType(),
        field.getReferencedBusinessEntityId()
    );
  }
}
