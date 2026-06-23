package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.FieldType;
import com.tfm.busonotec_backend.domain.RelationshipType;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.util.IdentifierValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntityFieldValidator {
  private static final Logger log = LoggerFactory.getLogger(EntityFieldValidator.class);

  public void validateForCreate(EntityFieldRequest request) {
    if (request == null) {
      validateName(null);
    }
    validateName(request.getName());
    FieldType fieldType = validateType(request.getType());
    validateValidationRules(fieldType, request);
    validateRelationshipRules(fieldType, request);
  }

  private void validateName(String name) {
    try {
      IdentifierValidator.requireValid(name, "Field name", "field name");
    } catch (IllegalArgumentException e) {
      if (name == null || name.isBlank()) {
        log.warn("Validation failed: field name blank");
      } else {
        log.warn("Validation failed: invalid field name {}", name);
      }
      throw e;
    }
    if ("id".equalsIgnoreCase(name)) {
      throw new IllegalArgumentException("Field name 'id' is reserved");
    }
  }

  private FieldType validateType(String type) {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Field type must be provided");
    }
    return FieldType.requireSupported(type, "Unsupported field type: ");
  }

  private void validateValidationRules(FieldType fieldType, EntityFieldRequest request) {
    validateTextRules(fieldType, request.getMinLength(), request.getMaxLength());
    validateNumberRules(fieldType, request.getMinValue(), request.getMaxValue());
    validateDateRules(fieldType, request);
  }

  private void validateTextRules(FieldType fieldType, Integer minLength, Integer maxLength) {
    if (fieldType != FieldType.STRING && (minLength != null || maxLength != null)) {
      throw new IllegalArgumentException("Length validations are only supported for string fields");
    }
    if (minLength != null && minLength < 0) {
      throw new IllegalArgumentException("Minimum length must be greater than or equal to 0");
    }
    if (maxLength != null && maxLength < 0) {
      throw new IllegalArgumentException("Maximum length must be greater than or equal to 0");
    }
    if (minLength != null && maxLength != null && minLength > maxLength) {
      throw new IllegalArgumentException("Minimum length must be less than or equal to maximum length");
    }
  }

  private void validateNumberRules(FieldType fieldType, BigDecimal minValue, BigDecimal maxValue) {
    if (fieldType != FieldType.NUMBER && (minValue != null || maxValue != null)) {
      throw new IllegalArgumentException("Numeric validations are only supported for number fields");
    }
    if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
      throw new IllegalArgumentException("Minimum value must be less than or equal to maximum value");
    }
  }

  private void validateDateRules(FieldType fieldType, EntityFieldRequest request) {
    if (fieldType != FieldType.DATE && (request.getMinDate() != null || request.getMaxDate() != null)) {
      throw new IllegalArgumentException("Date validations are only supported for date fields");
    }
    if (request.getMinDate() != null && request.getMaxDate() != null && request.getMinDate().isAfter(request.getMaxDate())) {
      throw new IllegalArgumentException("Minimum date must be less than or equal to maximum date");
    }
  }

  private void validateRelationshipRules(FieldType fieldType, EntityFieldRequest request) {
    if (fieldType != FieldType.RELATIONSHIP) {
      if (request.getRelationshipType() != null || request.getReferencedBusinessEntityId() != null) {
        throw new IllegalArgumentException("Relationship metadata is only supported for relationship fields");
      }
      return;
    }
    if (request.getMinLength() != null || request.getMaxLength() != null
        || request.getMinValue() != null || request.getMaxValue() != null
        || request.getMinDate() != null || request.getMaxDate() != null) {
      throw new IllegalArgumentException("Validation ranges are not supported for relationship fields");
    }
    if (request.getReferencedBusinessEntityId() == null) {
      throw new IllegalArgumentException("Referenced business entity id must be provided for relationship fields");
    }
    String normalizedRelationshipType = RelationshipType.normalize(request.getRelationshipType());
    if (normalizedRelationshipType == null) {
      throw new IllegalArgumentException("Relationship type must be provided");
    }
    if (RelationshipType.from(normalizedRelationshipType).isEmpty()) {
      throw new IllegalArgumentException("Unsupported relationship type: " + request.getRelationshipType());
    }
  }
}
