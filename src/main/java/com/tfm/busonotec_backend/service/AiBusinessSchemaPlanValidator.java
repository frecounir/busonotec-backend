package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.FieldType;
import com.tfm.busonotec_backend.domain.RelationshipType;
import com.tfm.busonotec_backend.dto.AiBusinessEntityDefinition;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import com.tfm.busonotec_backend.dto.AiEntityFieldDefinition;
import com.tfm.busonotec_backend.util.IdentifierValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AiBusinessSchemaPlanValidator {
  public void validate(AiBusinessSchemaPlan plan) {
    if (plan == null || plan.businessEntities() == null || plan.businessEntities().isEmpty()) {
      throw new IllegalArgumentException("AI response must include at least one business entity");
    }

    Set<String> entityNames = new HashSet<>();
    for (AiBusinessEntityDefinition entity : plan.businessEntities()) {
      if (entity == null) {
        throw new IllegalArgumentException("AI response contains an empty business entity");
      }
      validateIdentifier(entity.name(), "entity name");
      String entityKey = normalizeKey(entity.name());
      if (!entityNames.add(entityKey)) {
        throw new IllegalArgumentException("AI response contains duplicated entity: " + entity.name());
      }
    }

    for (AiBusinessEntityDefinition entity : plan.businessEntities()) {
      validateEntity(entity, entityNames);
    }
  }

  private void validateEntity(AiBusinessEntityDefinition entity, Set<String> entityNames) {
    Set<String> fieldNames = new HashSet<>();
    for (AiEntityFieldDefinition field : safeFields(entity)) {
      validateField(field, entityNames);
      String fieldKey = normalizeKey(field.name());
      if (!fieldNames.add(fieldKey)) {
        throw new IllegalArgumentException("AI response contains duplicated field for entity " + entity.name() + ": " + field.name());
      }
    }
  }

  private void validateField(AiEntityFieldDefinition field, Set<String> entityNames) {
    if (field == null) {
      throw new IllegalArgumentException("AI response contains an empty field");
    }
    validateIdentifier(field.name(), "field name");
    if ("id".equalsIgnoreCase(field.name())) {
      throw new IllegalArgumentException("AI response field name 'id' is reserved");
    }
    FieldType type = FieldType.from(field.type())
        .orElseThrow(() -> new IllegalArgumentException("AI response contains unsupported field type: " + field.type()));
    validateFieldRules(type, field);
    validateRelationshipRules(type, field, entityNames);
  }

  private void validateFieldRules(FieldType type, AiEntityFieldDefinition field) {
    validateTextRules(type, field.minLength(), field.maxLength());
    validateNumberRules(type, field.minValue(), field.maxValue());
    validateDateRules(type, field);
  }

  private void validateTextRules(FieldType type, Integer minLength, Integer maxLength) {
    if (type != FieldType.STRING && (minLength != null || maxLength != null)) {
      throw new IllegalArgumentException("AI response length validations are only supported for string fields");
    }
    if (minLength != null && minLength < 0) {
      throw new IllegalArgumentException("AI response minimum length must be greater than or equal to 0");
    }
    if (maxLength != null && maxLength < 0) {
      throw new IllegalArgumentException("AI response maximum length must be greater than or equal to 0");
    }
    if (minLength != null && maxLength != null && minLength > maxLength) {
      throw new IllegalArgumentException("AI response minimum length must be less than or equal to maximum length");
    }
  }

  private void validateNumberRules(FieldType type, BigDecimal minValue, BigDecimal maxValue) {
    if (type != FieldType.NUMBER && (minValue != null || maxValue != null)) {
      throw new IllegalArgumentException("AI response numeric validations are only supported for number fields");
    }
    if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
      throw new IllegalArgumentException("AI response minimum value must be less than or equal to maximum value");
    }
  }

  private void validateDateRules(FieldType type, AiEntityFieldDefinition field) {
    if (type != FieldType.DATE && (field.minDate() != null || field.maxDate() != null)) {
      throw new IllegalArgumentException("AI response date validations are only supported for date fields");
    }
    if (field.minDate() != null && field.maxDate() != null && field.minDate().isAfter(field.maxDate())) {
      throw new IllegalArgumentException("AI response minimum date must be less than or equal to maximum date");
    }
  }

  private void validateRelationshipRules(FieldType type, AiEntityFieldDefinition field, Set<String> entityNames) {
    if (type != FieldType.RELATIONSHIP) {
      if (field.relationshipType() != null || field.referencedEntityName() != null) {
        throw new IllegalArgumentException("AI response relationship metadata is only supported for relationship fields");
      }
      return;
    }
    if (field.minLength() != null || field.maxLength() != null
        || field.minValue() != null || field.maxValue() != null
        || field.minDate() != null || field.maxDate() != null) {
      throw new IllegalArgumentException("AI response validation ranges are not supported for relationship fields");
    }
    if (RelationshipType.normalize(field.relationshipType()) == null) {
      throw new IllegalArgumentException("AI response relationship type must be provided");
    }
    RelationshipType.from(field.relationshipType())
        .orElseThrow(() -> new IllegalArgumentException("AI response contains unsupported relationship type: " + field.relationshipType()));
    validateIdentifier(field.referencedEntityName(), "referenced entity name");
    String referencedEntityKey = normalizeKey(field.referencedEntityName());
    if (!entityNames.contains(referencedEntityKey)) {
      throw new IllegalArgumentException("AI response relationship references unknown entity: " + field.referencedEntityName());
    }
  }

  private void validateIdentifier(String value, String label) {
    IdentifierValidator.requireValid(value, "AI response " + label, "AI response " + label);
  }

  private List<AiEntityFieldDefinition> safeFields(AiBusinessEntityDefinition entity) {
    return entity.fields() == null ? List.of() : entity.fields();
  }

  private String normalizeKey(String value) {
    return value.toLowerCase(Locale.ROOT);
  }
}
