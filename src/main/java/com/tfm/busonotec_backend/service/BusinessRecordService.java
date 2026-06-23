package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;
import com.tfm.busonotec_backend.repository.BusinessRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BusinessRecordService {
  private final BusinessEntityRepository entityRepository;
  private final EntityFieldRepository fieldRepository;
  private final BusinessRecordRepository recordRepository;
  private final DynamicSchemaService dynamicSchemaService;
  private final BusinessRecordNormalizer normalizer;

  public BusinessRecordService(
      BusinessEntityRepository entityRepository,
      EntityFieldRepository fieldRepository,
      BusinessRecordRepository recordRepository,
      DynamicSchemaService dynamicSchemaService
  ) {
    this(entityRepository, fieldRepository, recordRepository, dynamicSchemaService, new BusinessRecordNormalizer());
  }

  @Autowired
  public BusinessRecordService(
      BusinessEntityRepository entityRepository,
      EntityFieldRepository fieldRepository,
      BusinessRecordRepository recordRepository,
      DynamicSchemaService dynamicSchemaService,
      BusinessRecordNormalizer normalizer
  ) {
    this.entityRepository = entityRepository;
    this.fieldRepository = fieldRepository;
    this.recordRepository = recordRepository;
    this.dynamicSchemaService = dynamicSchemaService;
    this.normalizer = normalizer;
  }

  public List<Map<String, Object>> listByBusinessEntity(UUID businessEntityId) {
    BusinessEntity entity = getBusinessEntityWithPhysicalTable(businessEntityId);
    return recordRepository.findAllByEntityName(entity.getName()).stream()
        .map(record -> normalizeOutputRecord(businessEntityId, record))
        .toList();
  }

  public Map<String, Object> create(UUID businessEntityId, Map<String, Object> record) {
    BusinessEntity entity = getBusinessEntityWithPhysicalTable(businessEntityId);
    Map<String, Object> valuesByColumn = normalizeRecordValues(businessEntityId, record, true);
    return normalizeOutputRecord(businessEntityId, recordRepository.create(entity.getName(), UUID.randomUUID(), valuesByColumn));
  }

  public Map<String, Object> update(UUID businessEntityId, UUID recordId, Map<String, Object> record) {
    BusinessEntity entity = getBusinessEntityWithPhysicalTable(businessEntityId);
    validateRecordId(recordId);
    Map<String, Object> valuesByColumn = normalizeRecordValues(businessEntityId, record, false);
    if (valuesByColumn.isEmpty()) {
      throw new IllegalArgumentException("Record body must include at least one field");
    }

    boolean updated = recordRepository.update(entity.getName(), recordId, valuesByColumn);
    if (!updated) {
      throw new IllegalArgumentException("Record not found: " + recordId);
    }
    return normalizeOutputRecord(businessEntityId, recordRepository.findByEntityNameAndId(entity.getName(), recordId));
  }

  public void delete(UUID businessEntityId, UUID recordId) {
    BusinessEntity entity = getBusinessEntityWithPhysicalTable(businessEntityId);
    validateRecordId(recordId);

    boolean deleted = recordRepository.delete(entity.getName(), recordId);
    if (!deleted) {
      throw new IllegalArgumentException("Record not found: " + recordId);
    }
  }

  private BusinessEntity getBusinessEntityWithPhysicalTable(UUID businessEntityId) {
    if (businessEntityId == null) {
      throw new IllegalArgumentException("Business entity id must be provided");
    }
    BusinessEntity entity = entityRepository.findById(businessEntityId)
        .orElseThrow(() -> new IllegalArgumentException("Business entity not found: " + businessEntityId));
    if (!dynamicSchemaService.entityExists(entity.getName())) {
      throw new IllegalArgumentException("Physical table does not exist for entity: " + entity.getName());
    }
    return entity;
  }

  private Map<String, Object> normalizeRecordValues(
      UUID businessEntityId,
      Map<String, Object> record,
      boolean validateRequiredFields
  ) {
    return normalizer.normalizeInput(record, fieldsByName(businessEntityId), validateRequiredFields);
  }

  private Map<String, Object> normalizeOutputRecord(UUID businessEntityId, Map<String, Object> record) {
    return normalizer.normalizeOutput(record, fieldsByName(businessEntityId));
  }

  private Map<String, EntityField> fieldsByName(UUID businessEntityId) {
    return fieldRepository.findByBusinessEntityId(businessEntityId).stream()
        .collect(Collectors.toMap(
            field -> field.getName().toLowerCase(Locale.ROOT),
            field -> field
        ));
  }

  private void validateRecordId(UUID recordId) {
    if (recordId == null) {
      throw new IllegalArgumentException("Record id must be provided");
    }
  }
}
