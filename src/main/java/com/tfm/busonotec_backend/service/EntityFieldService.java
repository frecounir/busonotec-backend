package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.domain.FieldType;
import com.tfm.busonotec_backend.domain.RelationshipType;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntityFieldService {
  private static final Logger log = LoggerFactory.getLogger(EntityFieldService.class);
  private final EntityFieldRepository repository;
  private final BusinessEntityRepository entityRepository;
  private final DynamicSchemaService dynamicSchemaService;
  private final EntityFieldValidator validator;
  private final EntityFieldMapper mapper;

  public EntityFieldService(EntityFieldRepository repository,
                            BusinessEntityRepository entityRepository,
                            DynamicSchemaService dynamicSchemaService) {
    this(repository, entityRepository, dynamicSchemaService, new EntityFieldValidator(), new EntityFieldMapper());
  }

  @Autowired
  public EntityFieldService(EntityFieldRepository repository,
                            BusinessEntityRepository entityRepository,
                            DynamicSchemaService dynamicSchemaService,
                            EntityFieldValidator validator,
                            EntityFieldMapper mapper) {
    this.repository = repository;
    this.entityRepository = entityRepository;
    this.dynamicSchemaService = dynamicSchemaService;
    this.validator = validator;
    this.mapper = mapper;
  }

  @Transactional
  public EntityFieldResponse create(EntityFieldRequest req) {
    validator.validateForCreate(req);
    UUID entityId = req.getBusinessEntityId();
    Optional<BusinessEntity> entity = entityId == null ? Optional.empty() : entityRepository.findById(entityId);
    if (entity.isEmpty()) {
      throw new IllegalArgumentException("BusinessEntity not found: " + entityId);
    }
    if (repository.existsByNameForEntity(entityId, req.getName())) {
      throw new IllegalArgumentException("Field with name already exists for entity: " + req.getName());
    }
    UUID id = UUID.randomUUID();
    String normalizedType = FieldType.normalize(req.getType());
    String normalizedRelationshipType = RelationshipType.normalize(req.getRelationshipType());
    Optional<BusinessEntity> referencedEntity = referencedEntity(req, normalizedType);
    EntityField f = mapper.toDomain(id, req, normalizedType, normalizedRelationshipType);
    if (FieldType.RELATIONSHIP.is(normalizedType)) {
      dynamicSchemaService.addRelationshipColumn(
          entity.get().getName(),
          req.getName(),
          referencedEntity.get().getName(),
          normalizedRelationshipType
      );
    } else {
      dynamicSchemaService.addColumn(entity.get().getName(), req.getName(), normalizedType);
    }
    repository.save(f);
    log.info("Created field {} for entity {}", req.getName(), entityId);
    return mapper.toResponse(f);
  }

  public List<EntityFieldResponse> listByEntity(UUID entityId) {
    List<EntityField> fields = repository.findByBusinessEntityId(entityId);
    List<EntityFieldResponse> out = new ArrayList<>();
    for (EntityField f : fields) {
      out.add(mapper.toResponse(f));
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

  private Optional<BusinessEntity> referencedEntity(EntityFieldRequest req, String normalizedType) {
    if (!FieldType.RELATIONSHIP.is(normalizedType)) {
      return Optional.empty();
    }
    Optional<BusinessEntity> referencedEntity = entityRepository.findById(req.getReferencedBusinessEntityId());
    if (referencedEntity.isEmpty()) {
      throw new IllegalArgumentException("Referenced BusinessEntity not found: " + req.getReferencedBusinessEntityId());
    }
    return referencedEntity;
  }
}
