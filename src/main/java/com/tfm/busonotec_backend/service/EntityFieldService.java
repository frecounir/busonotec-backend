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

import java.util.ArrayList;
import java.util.List;
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
    UUID entityId = req.getBusinessEntityId();
    Optional<BusinessEntity> entity = entityId == null ? Optional.empty() : entityRepository.findById(entityId);
    if (entity.isEmpty()) {
      throw new IllegalArgumentException("BusinessEntity not found: " + entityId);
    }
    if (repository.existsByNameForEntity(entityId, req.getName())) {
      throw new IllegalArgumentException("Field with name already exists for entity: " + req.getName());
    }
    UUID id = UUID.randomUUID();
    EntityField f = new EntityField(id, req.getName(), req.getType(), entityId, null);
    dynamicSchemaService.addColumn(entity.get().getName(), req.getName(), req.getType());
    repository.save(f);
    log.info("Created field {} for entity {}", req.getName(), entityId);
    return new EntityFieldResponse(id, entityId, req.getName(), req.getType());
  }

  public List<EntityFieldResponse> listByEntity(UUID entityId) {
    List<com.tfm.busonotec_backend.domain.EntityField> fields = repository.findByBusinessEntityId(entityId);
    List<EntityFieldResponse> out = new ArrayList<>();
    for (com.tfm.busonotec_backend.domain.EntityField f : fields) {
      out.add(new EntityFieldResponse(f.getId(), f.getBusinessEntityId(), f.getName(), f.getType()));
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
}
