package com.tfm.busonotec_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Core domain: EntityField represents a logical field belonging to a BusinessEntity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityField {
  private UUID id;
  private String name;
  private String type; // string, number, boolean, date, relationship
  private UUID businessEntityId;
  private FieldDetail detail;
  private boolean required;
  private Integer minLength;
  private Integer maxLength;
  private BigDecimal minValue;
  private BigDecimal maxValue;
  private LocalDate minDate;
  private LocalDate maxDate;
  private String relationshipType;
  private UUID referencedBusinessEntityId;

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
    this(id, name, type, businessEntityId, detail, required, minLength, maxLength, minValue, maxValue, minDate, maxDate, null, null);
  }
}
