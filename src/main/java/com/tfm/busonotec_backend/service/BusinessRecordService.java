package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;
import com.tfm.busonotec_backend.repository.BusinessRecordRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BusinessRecordService {
  private static final Pattern FIELD_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");

  private final BusinessEntityRepository entityRepository;
  private final EntityFieldRepository fieldRepository;
  private final BusinessRecordRepository recordRepository;
  private final DynamicSchemaService dynamicSchemaService;

  public BusinessRecordService(
      BusinessEntityRepository entityRepository,
      EntityFieldRepository fieldRepository,
      BusinessRecordRepository recordRepository,
      DynamicSchemaService dynamicSchemaService
  ) {
    this.entityRepository = entityRepository;
    this.fieldRepository = fieldRepository;
    this.recordRepository = recordRepository;
    this.dynamicSchemaService = dynamicSchemaService;
  }

  public List<Map<String, Object>> listByBusinessEntity(UUID businessEntityId) {
    BusinessEntity entity = getBusinessEntityWithPhysicalTable(businessEntityId);
    return recordRepository.findAllByEntityName(entity.getName());
  }

  public Map<String, Object> create(UUID businessEntityId, Map<String, Object> record) {
    BusinessEntity entity = getBusinessEntityWithPhysicalTable(businessEntityId);
    Map<String, Object> valuesByColumn = normalizeRecordValues(businessEntityId, record);
    return recordRepository.create(entity.getName(), UUID.randomUUID(), valuesByColumn);
  }

  public Map<String, Object> update(UUID businessEntityId, UUID recordId, Map<String, Object> record) {
    BusinessEntity entity = getBusinessEntityWithPhysicalTable(businessEntityId);
    validateRecordId(recordId);
    Map<String, Object> valuesByColumn = normalizeRecordValues(businessEntityId, record);
    if (valuesByColumn.isEmpty()) {
      throw new IllegalArgumentException("Record body must include at least one field");
    }

    boolean updated = recordRepository.update(entity.getName(), recordId, valuesByColumn);
    if (!updated) {
      throw new IllegalArgumentException("Record not found: " + recordId);
    }
    return recordRepository.findByEntityNameAndId(entity.getName(), recordId);
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

  private Map<String, Object> normalizeRecordValues(UUID businessEntityId, Map<String, Object> record) {
    if (record == null) {
      throw new IllegalArgumentException("Record body must be provided");
    }

    Set<String> allowedFields = fieldRepository.findByBusinessEntityId(businessEntityId).stream()
        .map(EntityField::getName)
        .map(name -> name.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
    Map<String, Object> normalized = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : record.entrySet()) {
      String normalizedFieldName = normalizeFieldName(entry.getKey());
      if (!allowedFields.contains(normalizedFieldName)) {
        throw new IllegalArgumentException("Field is not defined for entity: " + entry.getKey());
      }
      if (normalized.containsKey(normalizedFieldName)) {
        throw new IllegalArgumentException("Duplicate field in record: " + entry.getKey());
      }
      normalized.put(normalizedFieldName, entry.getValue());
    }

    return normalized;
  }

  private String normalizeFieldName(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("Record field name must be provided");
    }
    if (!FIELD_NAME.matcher(fieldName).matches()) {
      throw new IllegalArgumentException("Invalid record field name: " + fieldName);
    }
    if ("id".equalsIgnoreCase(fieldName)) {
      throw new IllegalArgumentException("Record field name 'id' is reserved");
    }
    return fieldName.toLowerCase(Locale.ROOT);
  }

  private void validateRecordId(UUID recordId) {
    if (recordId == null) {
      throw new IllegalArgumentException("Record id must be provided");
    }
  }
}
