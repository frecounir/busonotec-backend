package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;
import com.tfm.busonotec_backend.repository.BusinessRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    if (record == null) {
      throw new IllegalArgumentException("Record body must be provided");
    }

    Map<String, EntityField> fieldsByName = fieldsByName(businessEntityId);
    Map<String, Object> normalized = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : record.entrySet()) {
      String normalizedFieldName = normalizeFieldName(entry.getKey());
      EntityField field = fieldsByName.get(normalizedFieldName);
      if (field == null) {
        throw new IllegalArgumentException("Field is not defined for entity: " + entry.getKey());
      }
      if (normalized.containsKey(normalizedFieldName)) {
        throw new IllegalArgumentException("Duplicate field in record: " + entry.getKey());
      }
      normalized.put(normalizedFieldName, normalizeInputValue(normalizedFieldName, field, entry.getValue()));
    }

    if (validateRequiredFields) {
      validateRequiredFields(fieldsByName, normalized);
    }
    return normalized;
  }

  private Map<String, Object> normalizeOutputRecord(UUID businessEntityId, Map<String, Object> record) {
    Map<String, EntityField> fieldsByName = fieldsByName(businessEntityId);
    Map<String, Object> normalized = new LinkedHashMap<>();

    record.forEach((fieldName, value) -> {
      String normalizedFieldName = fieldName.toLowerCase(Locale.ROOT);
      EntityField field = fieldsByName.get(normalizedFieldName);
      normalized.put(normalizedFieldName, normalizeOutputValue(field == null ? null : field.getType(), value));
    });

    return normalized;
  }

  private Map<String, EntityField> fieldsByName(UUID businessEntityId) {
    return fieldRepository.findByBusinessEntityId(businessEntityId).stream()
        .collect(Collectors.toMap(
            field -> field.getName().toLowerCase(Locale.ROOT),
            field -> field
        ));
  }

  private Object normalizeInputValue(String fieldName, EntityField field, Object value) {
    if (value == null) {
      if (field.isRequired()) {
        throw new IllegalArgumentException("Required field must not be null: " + field.getName());
      }
      return null;
    }
    String fieldType = field.getType().toLowerCase(Locale.ROOT);
    return switch (fieldType) {
      case "string" -> validateStringValue(field, value);
      case "number" -> validateNumberValue(field, value);
      case "boolean" -> validateBooleanValue(field, value);
      case "date" -> validateDateValue(fieldName, field, value);
      case "relationship" -> validateRelationshipValue(field, value);
      default -> throw new IllegalArgumentException("Unsupported field type: " + field.getType());
    };
  }

  private Object validateStringValue(EntityField field, Object value) {
    if (!(value instanceof String textValue)) {
      throw new IllegalArgumentException("Invalid string value for field " + field.getName());
    }
    if (field.getMinLength() != null && textValue.length() < field.getMinLength()) {
      throw new IllegalArgumentException("String value for field " + field.getName() + " is shorter than minimum length " + field.getMinLength());
    }
    if (field.getMaxLength() != null && textValue.length() > field.getMaxLength()) {
      throw new IllegalArgumentException("String value for field " + field.getName() + " exceeds maximum length " + field.getMaxLength());
    }
    return textValue;
  }

  private Object validateNumberValue(EntityField field, Object value) {
    BigDecimal numberValue = toBigDecimal(field.getName(), value);
    if (field.getMinValue() != null && numberValue.compareTo(field.getMinValue()) < 0) {
      throw new IllegalArgumentException("Number value for field " + field.getName() + " is lower than minimum value " + field.getMinValue());
    }
    if (field.getMaxValue() != null && numberValue.compareTo(field.getMaxValue()) > 0) {
      throw new IllegalArgumentException("Number value for field " + field.getName() + " exceeds maximum value " + field.getMaxValue());
    }
    return value;
  }

  private Object validateBooleanValue(EntityField field, Object value) {
    if (!(value instanceof Boolean)) {
      throw new IllegalArgumentException("Invalid boolean value for field " + field.getName());
    }
    return value;
  }

  private LocalDate validateDateValue(String fieldName, EntityField field, Object value) {
    if (value instanceof LocalDate localDate) {
      return validateDateRange(field, localDate);
    }
    if (value instanceof String textValue) {
      try {
        return validateDateRange(field, LocalDate.parse(textValue));
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Invalid date value for field " + fieldName + ". Expected format: yyyy-MM-dd");
      }
    }
    throw new IllegalArgumentException("Invalid date value for field " + fieldName + ". Expected format: yyyy-MM-dd");
  }

  private UUID validateRelationshipValue(EntityField field, Object value) {
    if (value instanceof UUID uuid) {
      return uuid;
    }
    if (value instanceof String textValue) {
      try {
        return UUID.fromString(textValue);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid relationship value for field " + field.getName() + ". Expected UUID");
      }
    }
    throw new IllegalArgumentException("Invalid relationship value for field " + field.getName() + ". Expected UUID");
  }

  private LocalDate validateDateRange(EntityField field, LocalDate value) {
    if (field.getMinDate() != null && value.isBefore(field.getMinDate())) {
      throw new IllegalArgumentException("Date value for field " + field.getName() + " is before minimum date " + field.getMinDate());
    }
    if (field.getMaxDate() != null && value.isAfter(field.getMaxDate())) {
      throw new IllegalArgumentException("Date value for field " + field.getName() + " is after maximum date " + field.getMaxDate());
    }
    return value;
  }

  private BigDecimal toBigDecimal(String fieldName, Object value) {
    if (!(value instanceof Number numberValue)) {
      throw new IllegalArgumentException("Invalid number value for field " + fieldName);
    }
    if (numberValue instanceof Double doubleValue && !Double.isFinite(doubleValue)) {
      throw new IllegalArgumentException("Invalid number value for field " + fieldName);
    }
    if (numberValue instanceof Float floatValue && !Float.isFinite(floatValue)) {
      throw new IllegalArgumentException("Invalid number value for field " + fieldName);
    }
    try {
      return new BigDecimal(numberValue.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number value for field " + fieldName);
    }
  }

  private void validateRequiredFields(Map<String, EntityField> fieldsByName, Map<String, Object> normalizedRecord) {
    fieldsByName.forEach((normalizedName, field) -> {
      if (field.isRequired() && !normalizedRecord.containsKey(normalizedName)) {
        throw new IllegalArgumentException("Required field is missing: " + field.getName());
      }
    });
  }

  private Object normalizeOutputValue(String fieldType, Object value) {
    if (value == null || !"date".equals(fieldType)) {
      return value;
    }
    if (value instanceof LocalDate localDate) {
      return localDate;
    }
    if (value instanceof java.sql.Date date) {
      return date.toLocalDate();
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime().toLocalDate();
    }
    return value;
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
