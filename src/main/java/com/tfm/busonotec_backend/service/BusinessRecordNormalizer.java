package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.domain.FieldType;
import com.tfm.busonotec_backend.util.IdentifierValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class BusinessRecordNormalizer {
  public Map<String, Object> normalizeInput(
      Map<String, Object> record,
      Map<String, EntityField> fieldsByName,
      boolean validateRequiredFields
  ) {
    if (record == null) {
      throw new IllegalArgumentException("Record body must be provided");
    }

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

  public Map<String, Object> normalizeOutput(Map<String, Object> record, Map<String, EntityField> fieldsByName) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    record.forEach((fieldName, value) -> {
      String normalizedFieldName = fieldName.toLowerCase(Locale.ROOT);
      EntityField field = fieldsByName.get(normalizedFieldName);
      normalized.put(normalizedFieldName, normalizeOutputValue(field == null ? null : field.getType(), value));
    });
    return normalized;
  }

  private Object normalizeInputValue(String fieldName, EntityField field, Object value) {
    if (value == null) {
      if (field.isRequired()) {
        throw new IllegalArgumentException("Required field must not be null: " + field.getName());
      }
      return null;
    }
    FieldType fieldType = FieldType.requireSupported(field.getType(), "Unsupported field type: ");
    return switch (fieldType) {
      case STRING -> validateStringValue(field, value);
      case NUMBER -> validateNumberValue(field, value);
      case BOOLEAN -> validateBooleanValue(field, value);
      case DATE -> validateDateValue(fieldName, field, value);
      case RELATIONSHIP -> validateRelationshipValue(field, value);
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
    if (value == null || !FieldType.DATE.is(fieldType)) {
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
    IdentifierValidator.requireValid(fieldName, "Record field name", "record field name");
    if ("id".equalsIgnoreCase(fieldName)) {
      throw new IllegalArgumentException("Record field name 'id' is reserved");
    }
    return fieldName.toLowerCase(Locale.ROOT);
  }
}
