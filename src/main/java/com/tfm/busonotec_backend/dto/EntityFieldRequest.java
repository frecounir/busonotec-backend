package com.tfm.busonotec_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO to create an EntityField.
 */
@Getter
@Schema(description = "Payload used to create a dynamic field, relationship metadata, and physical table column.")
public class EntityFieldRequest {
  @Schema(
      description = "UUID of the business entity that owns the field.",
      example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      format = "uuid",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private final UUID businessEntityId;

  @Schema(
      description = "Field name. It becomes a physical table column.",
      example = "score",
      minLength = 1,
      maxLength = 63,
      pattern = "^[a-zA-Z][a-zA-Z0-9_]{0,62}$",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private final String name;

  @Schema(
      description = "Logical field type mapped to a database column type.",
      example = "number",
      allowableValues = {"string", "number", "boolean", "date", "relationship"},
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private final String type;

  @Schema(description = "Whether the field must be present and non-null when creating records.", example = "true")
  private final Boolean required;

  @Schema(description = "Minimum text length. Only valid for string fields.", example = "3", minimum = "0")
  private final Integer minLength;

  @Schema(description = "Maximum text length. Only valid for string fields.", example = "120", minimum = "0")
  private final Integer maxLength;

  @Schema(description = "Minimum numeric value. Only valid for number fields.", example = "0")
  private final BigDecimal minValue;

  @Schema(description = "Maximum numeric value. Only valid for number fields.", example = "100")
  private final BigDecimal maxValue;

  @Schema(description = "Minimum date value. Only valid for date fields.", example = "2026-01-01", format = "date")
  private final LocalDate minDate;

  @Schema(description = "Maximum date value. Only valid for date fields.", example = "2026-12-31", format = "date")
  private final LocalDate maxDate;

  @Schema(
      description = "Relationship cardinality when type is relationship. Use many_to_one for foreign keys from many source records to one target record; one_to_one also creates a unique constraint.",
      example = "many_to_one",
      allowableValues = {"many_to_one", "one_to_one"}
  )
  private final String relationshipType;

  @Schema(
      description = "UUID of the target business entity when type is relationship.",
      example = "7fb85f64-5717-4562-b3fc-2c963f66afa6",
      format = "uuid"
  )
  private final UUID referencedBusinessEntityId;

  public EntityFieldRequest(UUID businessEntityId, String name, String type) {
    this(businessEntityId, name, type, null, null, null, null, null, null, null, null, null);
  }

  public EntityFieldRequest(UUID businessEntityId,
                            String name,
                            String type,
                            Boolean required,
                            Integer minLength,
                            Integer maxLength,
                            BigDecimal minValue,
                            BigDecimal maxValue,
                            LocalDate minDate,
                            LocalDate maxDate) {
    this(businessEntityId, name, type, required, minLength, maxLength, minValue, maxValue, minDate, maxDate, null, null);
  }

  @JsonCreator
  public EntityFieldRequest(@JsonProperty("businessEntityId") UUID businessEntityId,
                            @JsonProperty("name") String name,
                            @JsonProperty("type") String type,
                            @JsonProperty("required") Boolean required,
                            @JsonProperty("minLength") Integer minLength,
                            @JsonProperty("maxLength") Integer maxLength,
                            @JsonProperty("minValue") BigDecimal minValue,
                            @JsonProperty("maxValue") BigDecimal maxValue,
                            @JsonProperty("minDate") LocalDate minDate,
                            @JsonProperty("maxDate") LocalDate maxDate,
                            @JsonProperty("relationshipType") String relationshipType,
                            @JsonProperty("referencedBusinessEntityId") UUID referencedBusinessEntityId) {
    this.businessEntityId = businessEntityId;
    this.name = name;
    this.type = type;
    this.required = required;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.minDate = minDate;
    this.maxDate = maxDate;
    this.relationshipType = relationshipType;
    this.referencedBusinessEntityId = referencedBusinessEntityId;
  }
}
