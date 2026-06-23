package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessEntityService {
  private final BusinessEntityRepository repository;
  private final EntityFieldRepository fieldRepository;
  private final DynamicSchemaService dynamicSchemaService;
  private final BusinessEntityValidator validator;
  private final BusinessEntityMapper mapper;

  public BusinessEntityService(
      BusinessEntityRepository repository,
      EntityFieldRepository fieldRepository,
      DynamicSchemaService dynamicSchemaService
  ) {
    this(repository, fieldRepository, dynamicSchemaService, new BusinessEntityValidator(), new BusinessEntityMapper());
  }

  @Autowired
  public BusinessEntityService(
      BusinessEntityRepository repository,
      EntityFieldRepository fieldRepository,
      DynamicSchemaService dynamicSchemaService,
      BusinessEntityValidator validator,
      BusinessEntityMapper mapper
  ) {
    this.repository = repository;
    this.fieldRepository = fieldRepository;
    this.dynamicSchemaService = dynamicSchemaService;
    this.validator = validator;
    this.mapper = mapper;
  }

  public BusinessEntityResponse create(BusinessEntityRequest req) {
    validator.validateForCreate(req);
    if (repository.existsByName(req.getName())) {
      throw new IllegalArgumentException("Entity with name already exists: " + req.getName());
    }
    UUID id = UUID.randomUUID();
    BusinessEntity entity = mapper.toDomain(id, req);
    repository.save(entity);
    dynamicSchemaService.createEntityTable(req.getName());
    return mapper.toResponse(entity);
  }

  public List<BusinessEntityResponse> list() {
    List<BusinessEntity> list = repository.findAll();
    List<BusinessEntityResponse> out = new ArrayList<>();
    for (BusinessEntity entity : list) {
      out.add(mapper.toResponse(entity));
    }
    return out;
  }

  public BusinessEntityResponse findById(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("Business entity id must be provided");
    }
    BusinessEntity entity = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Business entity not found: " + id));
    return mapper.toResponse(entity);
  }

  @Transactional
  public void delete(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("Business entity id must be provided");
    }
    BusinessEntity entity = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Business entity not found: " + id));

    deleteIncomingRelationshipFields(id);
    fieldRepository.deleteByBusinessEntityId(id);
    boolean deleted = repository.deleteById(id);
    if (!deleted) {
      throw new IllegalArgumentException("Business entity not found: " + id);
    }
    dynamicSchemaService.dropEntityTable(entity.getName());
  }

  private void deleteIncomingRelationshipFields(UUID referencedBusinessEntityId) {
    List<EntityField> incomingRelationshipFields = fieldRepository.findByReferencedBusinessEntityId(referencedBusinessEntityId);
    for (EntityField field : incomingRelationshipFields) {
      repository.findById(field.getBusinessEntityId()).ifPresent(owner -> {
        if (dynamicSchemaService.entityExists(owner.getName())) {
          dynamicSchemaService.dropColumn(owner.getName(), field.getName());
        }
      });
      fieldRepository.deleteById(field.getId());
    }
  }
}
