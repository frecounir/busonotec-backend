package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BusinessEntityService {
  private static final Logger log = LoggerFactory.getLogger(BusinessEntityService.class);
  private static final String NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]{0,62}$";
  private final BusinessEntityRepository repository;
  private final DynamicSchemaService dynamicSchemaService;

  public BusinessEntityService(BusinessEntityRepository repository, DynamicSchemaService dynamicSchemaService) {
    this.repository = repository;
    this.dynamicSchemaService = dynamicSchemaService;
  }

  public BusinessEntityResponse create(BusinessEntityRequest req) {
    validateName(req.getName());
    if (repository.existsByName(req.getName())) {
      throw new IllegalArgumentException("Entity with name already exists: " + req.getName());
    }
    UUID id = UUID.randomUUID();
    BusinessEntity e = new BusinessEntity(id, req.getName(), req.getDescription());
    repository.save(e);
    // create physical table
    String tableName = req.getName().toLowerCase();
    String quoted = "\"" + tableName + "\"";
    String sql = "CREATE TABLE IF NOT EXISTS " + quoted + " (id UUID PRIMARY KEY)";
    Map<String, String> map = Collections.singletonMap(req.getName(), sql);
    log.info("Creating physical table for entity {}: {}", req.getName(), sql);
    dynamicSchemaService.executeStatements(map);
    return new BusinessEntityResponse(id, req.getName(), req.getDescription());
  }

  public List<BusinessEntityResponse> list() {
    List<BusinessEntity> list = repository.findAll();
    List<BusinessEntityResponse> out = new ArrayList<>();
    for (BusinessEntity e : list) out.add(toResponse(e));
    return out;
  }

  public BusinessEntityResponse findById(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("Business entity id must be provided");
    }
    BusinessEntity entity = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Business entity not found: " + id));
    return toResponse(entity);
  }

  private BusinessEntityResponse toResponse(BusinessEntity entity) {
    return new BusinessEntityResponse(entity.getId(), entity.getName(), entity.getDescription());
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      log.warn("Validation failed: entity name blank");
      throw new IllegalArgumentException("Entity name must be provided");
    }
    if (!name.matches(NAME_REGEX)) {
      log.warn("Validation failed: invalid entity name {}", name);
      throw new IllegalArgumentException("Invalid entity name: " + name);
    }
  }
}
