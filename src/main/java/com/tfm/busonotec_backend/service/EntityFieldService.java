package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntityFieldService {
  private static final Logger log = LoggerFactory.getLogger(EntityFieldService.class);
  private static final String NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]{0,62}$";
  private final EntityFieldRepository repository;
  private final BusinessEntityRepository entityRepository;
  private final DynamicSchemaService dynamicSchemaService;

  public EntityFieldService(EntityFieldRepository repository,
                            BusinessEntityRepository entityRepository,
                            DynamicSchemaService dynamicSchemaService) {
    this.repository = repository;
    this.entityRepository = entityRepository;
    this.dynamicSchemaService = dynamicSchemaService;
  }

  @Transactional
  public EntityFieldResponse create(EntityFieldRequest req) {
    validateName(req.getName());
    validateType(req.getType());
    validateValidationRules(req);
    UUID entityId = req.getBusinessEntityId();
    Optional<BusinessEntity> entity = entityId == null ? Optional.empty() : entityRepository.findById(entityId);
    if (entity.isEmpty()) {
      throw new IllegalArgumentException("BusinessEntity not found: " + entityId);
    }
    if (repository.existsByNameForEntity(entityId, req.getName())) {
      throw new IllegalArgumentException("Field with name already exists for entity: " + req.getName());
    }
    UUID id = UUID.randomUUID();
    String normalizedType = req.getType().toLowerCase(Locale.ROOT);
    EntityField f = new EntityField(
        id,
        req.getName(),
        normalizedType,
        entityId,
        null,
        required(req),
        req.getMinLength(),
        req.getMaxLength(),
        req.getMinValue(),
        req.getMaxValue(),
        req.getMinDate(),
        req.getMaxDate()
    );
    dynamicSchemaService.addColumn(entity.get().getName(), req.getName(), normalizedType);
    repository.save(f);
    log.info("Created field {} for entity {}", req.getName(), entityId);
    return toResponse(f);
  }

  public List<EntityFieldResponse> listByEntity(UUID entityId) {
    List<com.tfm.busonotec_backend.domain.EntityField> fields = repository.findByBusinessEntityId(entityId);
    List<EntityFieldResponse> out = new ArrayList<>();
    for (com.tfm.busonotec_backend.domain.EntityField f : fields) {
      out.add(toResponse(f));
    }
    return out;
  }

  @Transactional
  public void delete(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("Entity field id must be provided");
    }
    EntityField field = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Entity field not found: " + id));
    BusinessEntity entity = entityRepository.findById(field.getBusinessEntityId())
        .orElseThrow(() -> new IllegalArgumentException("BusinessEntity not found: " + field.getBusinessEntityId()));

    dynamicSchemaService.dropColumn(entity.getName(), field.getName());
    if (!repository.deleteById(id)) {
      throw new IllegalArgumentException("Entity field not found: " + id);
    }
    log.info("Deleted field {} from entity {}", field.getName(), field.getBusinessEntityId());
  }

  private EntityFieldResponse toResponse(EntityField field) {
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
        field.getMaxDate()
    );
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      log.warn("Validation failed: field name blank");
      throw new IllegalArgumentException("Field name must be provided");
    }
    if (!name.matches(NAME_REGEX)) {
      log.warn("Validation failed: invalid field name {}", name);
      throw new IllegalArgumentException("Invalid field name: " + name);
    }
    if ("id".equalsIgnoreCase(name)) {
      throw new IllegalArgumentException("Field name 'id' is reserved");
    }
  }

  private void validateType(String type) {
    if (type == null || type.isBlank()) throw new IllegalArgumentException("Field type must be provided");
    String t = type.toLowerCase();
    if (!(t.equals("string") || t.equals("number") || t.equals("boolean") || t.equals("date"))) {
      throw new IllegalArgumentException("Unsupported field type: " + type);
    }
  }

  private void validateValidationRules(EntityFieldRequest req) {
    String type = req.getType().toLowerCase(Locale.ROOT);
    validateTextRules(type, req.getMinLength(), req.getMaxLength());
    validateNumberRules(type, req.getMinValue(), req.getMaxValue());
    validateDateRules(type, req);
  }

  private void validateTextRules(String type, Integer minLength, Integer maxLength) {
    if (!"string".equals(type) && (minLength != null || maxLength != null)) {
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

  private void validateNumberRules(String type, BigDecimal minValue, BigDecimal maxValue) {
    if (!"number".equals(type) && (minValue != null || maxValue != null)) {
      throw new IllegalArgumentException("Numeric validations are only supported for number fields");
    }
    if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
      throw new IllegalArgumentException("Minimum value must be less than or equal to maximum value");
    }
  }

  private void validateDateRules(String type, EntityFieldRequest req) {
    if (!"date".equals(type) && (req.getMinDate() != null || req.getMaxDate() != null)) {
      throw new IllegalArgumentException("Date validations are only supported for date fields");
    }
    if (req.getMinDate() != null && req.getMaxDate() != null && req.getMinDate().isAfter(req.getMaxDate())) {
      throw new IllegalArgumentException("Minimum date must be less than or equal to maximum date");
    }
  }

  private boolean required(EntityFieldRequest req) {
    return Boolean.TRUE.equals(req.getRequired());
  }
}
