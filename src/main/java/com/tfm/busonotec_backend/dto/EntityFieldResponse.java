package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO returned after creating or listing fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dynamic entity field metadata returned by the API.")
public class EntityFieldResponse {
  @Schema(
      description = "Entity field UUID.",
      example = "c5ad7a80-63bb-4b0f-9679-2c0b1f5fcf9d",
      format = "uuid",
      accessMode = Schema.AccessMode.READ_ONLY
  )
  private UUID id;

  @Schema(
      description = "UUID of the business entity that owns the field.",
      example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      format = "uuid"
  )
  private UUID businessEntityId;

  @Schema(description = "Field name.", example = "score")
  private String name;

  @Schema(
      description = "Logical field type.",
      example = "number",
      allowableValues = {"string", "number", "boolean", "date", "relationship"}
  )
  private String type;

  @Schema(description = "Whether the field is required when creating records.", example = "true")
  private boolean required;

  @Schema(description = "Minimum text length for string fields.", example = "3")
  private Integer minLength;

  @Schema(description = "Maximum text length for string fields.", example = "120")
  private Integer maxLength;

  @Schema(description = "Minimum numeric value for number fields.", example = "0")
  private BigDecimal minValue;

  @Schema(description = "Maximum numeric value for number fields.", example = "100")
  private BigDecimal maxValue;

  @Schema(description = "Minimum date value for date fields.", example = "2026-01-01", format = "date")
  private LocalDate minDate;

  @Schema(description = "Maximum date value for date fields.", example = "2026-12-31", format = "date")
  private LocalDate maxDate;

  @Schema(
      description = "Relationship cardinality when this field references another business entity.",
      example = "many_to_one",
      allowableValues = {"many_to_one", "one_to_one"}
  )
  private String relationshipType;

  @Schema(
      description = "UUID of the referenced business entity when this field is a relationship.",
      example = "7fb85f64-5717-4562-b3fc-2c963f66afa6",
      format = "uuid"
  )
  private UUID referencedBusinessEntityId;

  public EntityFieldResponse(UUID id, UUID businessEntityId, String name, String type) {
    this(id, businessEntityId, name, type, false, null, null, null, null, null, null);
  }

  public EntityFieldResponse(UUID id,
                             UUID businessEntityId,
                             String name,
                             String type,
                             boolean required,
                             Integer minLength,
                             Integer maxLength,
                             BigDecimal minValue,
                             BigDecimal maxValue,
                             LocalDate minDate,
                             LocalDate maxDate) {
    this(id, businessEntityId, name, type, required, minLength, maxLength, minValue, maxValue, minDate, maxDate, null, null);
  }
}
