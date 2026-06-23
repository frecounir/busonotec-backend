package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.FieldType;
import com.tfm.busonotec_backend.domain.RelationshipType;
import com.tfm.busonotec_backend.dto.AiBusinessEntityDefinition;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import com.tfm.busonotec_backend.dto.AiEntityFieldDefinition;
import org.springframework.stereotype.Component;

@Component
public class AiBusinessSchemaPlanNormalizer {
  public AiBusinessSchemaPlan normalize(AiBusinessSchemaPlan plan) {
    if (plan == null || plan.businessEntities() == null) {
      return plan;
    }
    return new AiBusinessSchemaPlan(plan.businessEntities().stream()
        .map(this::normalizeEntity)
        .toList());
  }

  private AiBusinessEntityDefinition normalizeEntity(AiBusinessEntityDefinition entity) {
    if (entity == null || entity.fields() == null) {
      return entity;
    }
    return new AiBusinessEntityDefinition(
        entity.name(),
        entity.description(),
        entity.fields().stream()
            .map(this::normalizeField)
            .toList()
    );
  }

  private AiEntityFieldDefinition normalizeField(AiEntityFieldDefinition field) {
    if (field == null || field.type() == null) {
      return field;
    }
    String type = FieldType.normalize(field.type());
    return switch (type) {
      case "string" -> new AiEntityFieldDefinition(
          field.name(), type, field.required(), field.minLength(), field.maxLength(), null, null, null, null
      );
      case "number" -> new AiEntityFieldDefinition(
          field.name(), type, field.required(), null, null, field.minValue(), field.maxValue(), null, null
      );
      case "date" -> new AiEntityFieldDefinition(
          field.name(), type, field.required(), null, null, null, null, field.minDate(), field.maxDate()
      );
      case "boolean" -> new AiEntityFieldDefinition(
          field.name(), type, field.required(), null, null, null, null, null, null
      );
      case "relationship" -> new AiEntityFieldDefinition(
          field.name(),
          type,
          field.required(),
          null,
          null,
          null,
          null,
          null,
          null,
          RelationshipType.normalize(field.relationshipType()),
          field.referencedEntityName()
      );
      default -> field;
    };
  }
}
