package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EntityFieldService {
  private static final Logger log = LoggerFactory.getLogger(EntityFieldService.class);
  private static final String NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]{0,62}$";
  private final EntityFieldRepository repository;
  private final BusinessEntityRepository entityRepository;

  public EntityFieldService(EntityFieldRepository repository, BusinessEntityRepository entityRepository) {
    this.repository = repository;
    this.entityRepository = entityRepository;
  }

  public EntityFieldResponse create(EntityFieldRequest req) {
    validateName(req.getName());
    validateType(req.getType());
    // ensure parent exists
    UUID entityId = req.getBusinessEntityId();
    if (entityId == null || entityRepository.findById(entityId).isEmpty()) {
      throw new IllegalArgumentException("BusinessEntity not found: " + entityId);
    }
    if (repository.existsByNameForEntity(entityId, req.getName())) {
      throw new IllegalArgumentException("Field with name already exists for entity: " + req.getName());
    }
    UUID id = UUID.randomUUID();
    EntityField f = new EntityField(id, req.getName(), req.getType(), entityId, null);
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

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      log.warn("Validation failed: field name blank");
      throw new IllegalArgumentException("Field name must be provided");
    }
    if (!name.matches(NAME_REGEX)) {
      log.warn("Validation failed: invalid field name {}", name);
      throw new IllegalArgumentException("Invalid field name: " + name);
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
