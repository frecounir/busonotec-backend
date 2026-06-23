package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.FieldType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AiBusinessSchemaService {
  private final GenerativeAgentClient agentClient;
  private final BusinessEntityService businessEntityService;
  private final EntityFieldService entityFieldService;
  private final AiBusinessSchemaPlanNormalizer planNormalizer;
  private final AiBusinessSchemaPlanValidator planValidator;

  public AiBusinessSchemaService(
      GenerativeAgentClient agentClient,
      BusinessEntityService businessEntityService,
      EntityFieldService entityFieldService
  ) {
    this(
        agentClient,
        businessEntityService,
        entityFieldService,
        new AiBusinessSchemaPlanNormalizer(),
        new AiBusinessSchemaPlanValidator()
    );
  }

  @Autowired
  public AiBusinessSchemaService(
      GenerativeAgentClient agentClient,
      BusinessEntityService businessEntityService,
      EntityFieldService entityFieldService,
      AiBusinessSchemaPlanNormalizer planNormalizer,
      AiBusinessSchemaPlanValidator planValidator
  ) {
    this.agentClient = agentClient;
    this.businessEntityService = businessEntityService;
    this.entityFieldService = entityFieldService;
    this.planNormalizer = planNormalizer;
    this.planValidator = planValidator;
  }

  public AiBusinessSchemaPlan createPlanFromPrompt(AiBusinessSchemaRequest request) {
    String prompt = request == null ? null : request.prompt();
    validatePrompt(prompt);

    AiBusinessSchemaPlan plan = planNormalizer.normalize(agentClient.generateBusinessSchema(prompt));
    planValidator.validate(plan);
    return plan;
  }

  @Transactional
  public AiBusinessSchemaResponse executePlan(AiBusinessSchemaPlan plan) {
    AiBusinessSchemaPlan normalizedPlan = planNormalizer.normalize(plan);
    planValidator.validate(normalizedPlan);

    Map<String, BusinessEntityResponse> createdByName = new LinkedHashMap<>();
    for (AiBusinessEntityDefinition entityDefinition : normalizedPlan.businessEntities()) {
      BusinessEntityResponse entity = businessEntityService.create(
          new BusinessEntityRequest(entityDefinition.name(), entityDefinition.description())
      );
      createdByName.put(entityDefinition.name().toLowerCase(Locale.ROOT), entity);
    }

    List<CreatedBusinessEntityResponse> createdEntities = new ArrayList<>();
    for (AiBusinessEntityDefinition entityDefinition : normalizedPlan.businessEntities()) {
      BusinessEntityResponse entity = createdByName.get(entityDefinition.name().toLowerCase(Locale.ROOT));
      List<EntityFieldResponse> fields = createFields(entity.getId(), safeFields(entityDefinition), createdByName);
      createdEntities.add(new CreatedBusinessEntityResponse(entity, fields));
    }
    return new AiBusinessSchemaResponse(normalizedPlan, createdEntities);
  }

  private List<EntityFieldResponse> createFields(
      UUID businessEntityId,
      List<AiEntityFieldDefinition> fields,
      Map<String, BusinessEntityResponse> createdByName
  ) {
    List<EntityFieldResponse> createdFields = new ArrayList<>();
    for (AiEntityFieldDefinition field : fields) {
      UUID referencedBusinessEntityId = referencedBusinessEntityId(field, createdByName);
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
              field.maxDate(),
              field.relationshipType(),
              referencedBusinessEntityId
          )
      ));
    }
    return createdFields;
  }

  private UUID referencedBusinessEntityId(
      AiEntityFieldDefinition field,
      Map<String, BusinessEntityResponse> createdByName
  ) {
    if (!FieldType.RELATIONSHIP.is(field.type())) {
      return null;
    }
    BusinessEntityResponse referencedEntity = createdByName.get(field.referencedEntityName().toLowerCase(Locale.ROOT));
    if (referencedEntity == null) {
      throw new IllegalArgumentException("AI response relationship references unknown entity: " + field.referencedEntityName());
    }
    return referencedEntity.getId();
  }

  private void validatePrompt(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt must be provided");
    }
  }

  private List<AiEntityFieldDefinition> safeFields(AiBusinessEntityDefinition entity) {
    if (entity.fields() == null) {
      return List.of();
    }
    return entity.fields();
  }
}
