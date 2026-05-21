package com.tfm.busonotec_backend.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Core domain: EntityField represents a logical field belonging to a BusinessEntity.
 */
public class EntityField {
  private UUID id;
  private String name;
  private String type; // string, number, boolean, date
  private UUID businessEntityId;
  private FieldDetail detail;
  private boolean required;
  private Integer minLength;
  private Integer maxLength;
  private BigDecimal minValue;
  private BigDecimal maxValue;
  private LocalDate minDate;
  private LocalDate maxDate;

  public EntityField() {}

  public EntityField(UUID id, String name, String type, UUID businessEntityId, FieldDetail detail) {
    this(id, name, type, businessEntityId, detail, false, null, null, null, null, null, null);
  }

  public EntityField(UUID id,
                     String name,
                     String type,
                     UUID businessEntityId,
                     FieldDetail detail,
                     boolean required,
                     Integer minLength,
                     Integer maxLength,
                     BigDecimal minValue,
                     BigDecimal maxValue,
                     LocalDate minDate,
                     LocalDate maxDate) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.businessEntityId = businessEntityId;
    this.detail = detail;
    this.required = required;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.minDate = minDate;
    this.maxDate = maxDate;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public UUID getBusinessEntityId() { return businessEntityId; }
  public void setBusinessEntityId(UUID businessEntityId) { this.businessEntityId = businessEntityId; }

  public FieldDetail getDetail() { return detail; }
  public void setDetail(FieldDetail detail) { this.detail = detail; }

  public boolean isRequired() { return required; }
  public void setRequired(boolean required) { this.required = required; }

  public Integer getMinLength() { return minLength; }
  public void setMinLength(Integer minLength) { this.minLength = minLength; }

  public Integer getMaxLength() { return maxLength; }
  public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

  public BigDecimal getMinValue() { return minValue; }
  public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }

  public BigDecimal getMaxValue() { return maxValue; }
  public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }

  public LocalDate getMinDate() { return minDate; }
  public void setMinDate(LocalDate minDate) { this.minDate = minDate; }

  public LocalDate getMaxDate() { return maxDate; }
  public void setMaxDate(LocalDate maxDate) { this.maxDate = maxDate; }
}
