package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.dto.AiBusinessEntityDefinition;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaRequest;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaResponse;
import com.tfm.busonotec_backend.dto.AiEntityFieldDefinition;
import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.dto.CreatedBusinessEntityResponse;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiBusinessSchemaService {
  private static final Pattern NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");
  private static final Set<String> FIELD_TYPES = Set.of("string", "number", "boolean", "date");

  private final GenerativeAgentClient agentClient;
  private final BusinessEntityService businessEntityService;
  private final EntityFieldService entityFieldService;

  public AiBusinessSchemaService(
      GenerativeAgentClient agentClient,
      BusinessEntityService businessEntityService,
      EntityFieldService entityFieldService
  ) {
    this.agentClient = agentClient;
    this.businessEntityService = businessEntityService;
    this.entityFieldService = entityFieldService;
  }

  public AiBusinessSchemaPlan createPlanFromPrompt(AiBusinessSchemaRequest request) {
    String prompt = request == null ? null : request.prompt();
    validatePrompt(prompt);

    AiBusinessSchemaPlan plan = normalizePlan(agentClient.generateBusinessSchema(prompt));
    validatePlan(plan);
    return plan;
  }

  @Transactional
  public AiBusinessSchemaResponse executePlan(AiBusinessSchemaPlan plan) {
    AiBusinessSchemaPlan normalizedPlan = normalizePlan(plan);
    validatePlan(normalizedPlan);
    List<CreatedBusinessEntityResponse> createdEntities = new ArrayList<>();
    for (AiBusinessEntityDefinition entityDefinition : normalizedPlan.businessEntities()) {
      BusinessEntityResponse entity = businessEntityService.create(
          new BusinessEntityRequest(entityDefinition.name(), entityDefinition.description())
      );
      List<EntityFieldResponse> fields = createFields(entity.getId(), safeFields(entityDefinition));
      createdEntities.add(new CreatedBusinessEntityResponse(entity, fields));
    }
    return new AiBusinessSchemaResponse(normalizedPlan, createdEntities);
  }

  private List<EntityFieldResponse> createFields(
      java.util.UUID businessEntityId,
      List<AiEntityFieldDefinition> fields
  ) {
    List<EntityFieldResponse> createdFields = new ArrayList<>();
    for (AiEntityFieldDefinition field : fields) {
      createdFields.add(entityFieldService.create(
          new EntityFieldRequest(
              businessEntityId,
              field.name(),
              field.type(),
              field.required(),
              field.minLength(),
              field.maxLength(),
              field.minValue(),
              field.maxValue(),
              field.minDate(),
              field.maxDate()
          )
      ));
    }
    return createdFields;
  }

  private void validatePrompt(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt must be provided");
    }
  }

  private AiBusinessSchemaPlan normalizePlan(AiBusinessSchemaPlan plan) {
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
    String type = field.type().toLowerCase(Locale.ROOT);
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
      default -> field;
    };
  }

  private void validatePlan(AiBusinessSchemaPlan plan) {
    if (plan == null || plan.businessEntities() == null || plan.businessEntities().isEmpty()) {
      throw new IllegalArgumentException("AI response must include at least one business entity");
    }

    Set<String> entityNames = new HashSet<>();
    for (AiBusinessEntityDefinition entity : plan.businessEntities()) {
      validateEntity(entity);
      String entityKey = entity.name().toLowerCase(Locale.ROOT);
      if (!entityNames.add(entityKey)) {
        throw new IllegalArgumentException("AI response contains duplicated entity: " + entity.name());
      }
    }
  }

  private void validateEntity(AiBusinessEntityDefinition entity) {
    if (entity == null) {
      throw new IllegalArgumentException("AI response contains an empty business entity");
    }
    validateIdentifier(entity.name(), "entity name");

    Set<String> fieldNames = new HashSet<>();
    for (AiEntityFieldDefinition field : safeFields(entity)) {
      validateField(field);
      String fieldKey = field.name().toLowerCase(Locale.ROOT);
      if (!fieldNames.add(fieldKey)) {
        throw new IllegalArgumentException("AI response contains duplicated field for entity " + entity.name() + ": " + field.name());
      }
    }
  }

  private void validateField(AiEntityFieldDefinition field) {
    if (field == null) {
      throw new IllegalArgumentException("AI response contains an empty field");
    }
    validateIdentifier(field.name(), "field name");
    if ("id".equalsIgnoreCase(field.name())) {
      throw new IllegalArgumentException("AI response field name 'id' is reserved");
    }
    if (field.type() == null || !FIELD_TYPES.contains(field.type().toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("AI response contains unsupported field type: " + field.type());
    }
    validateFieldRules(field);
  }

  private void validateFieldRules(AiEntityFieldDefinition field) {
    String type = field.type().toLowerCase(Locale.ROOT);
    validateTextRules(type, field.minLength(), field.maxLength());
    validateNumberRules(type, field.minValue(), field.maxValue());
    validateDateRules(type, field);
  }

  private void validateTextRules(String type, Integer minLength, Integer maxLength) {
    if (!"string".equals(type) && (minLength != null || maxLength != null)) {
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

  private void validateNumberRules(String type, BigDecimal minValue, BigDecimal maxValue) {
    if (!"number".equals(type) && (minValue != null || maxValue != null)) {
      throw new IllegalArgumentException("AI response numeric validations are only supported for number fields");
    }
    if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
      throw new IllegalArgumentException("AI response minimum value must be less than or equal to maximum value");
    }
  }

  private void validateDateRules(String type, AiEntityFieldDefinition field) {
    if (!"date".equals(type) && (field.minDate() != null || field.maxDate() != null)) {
      throw new IllegalArgumentException("AI response date validations are only supported for date fields");
    }
    if (field.minDate() != null && field.maxDate() != null && field.minDate().isAfter(field.maxDate())) {
      throw new IllegalArgumentException("AI response minimum date must be less than or equal to maximum date");
    }
  }

  private void validateIdentifier(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("AI response " + label + " must be provided");
    }
    if (!NAME.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid AI response " + label + ": " + value);
    }
  }

  private List<AiEntityFieldDefinition> safeFields(AiBusinessEntityDefinition entity) {
    if (entity.fields() == null) {
      return List.of();
    }
    return entity.fields();
  }
}
