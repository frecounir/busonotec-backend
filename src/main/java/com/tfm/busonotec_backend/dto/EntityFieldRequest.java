package com.tfm.busonotec_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO to create an EntityField.
 */
@Schema(description = "Payload used to create a dynamic field and physical table column.")
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
      allowableValues = {"string", "number", "boolean", "date"},
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private final String type;

  @JsonCreator
  public EntityFieldRequest(@JsonProperty("businessEntityId") UUID businessEntityId,
                            @JsonProperty("name") String name,
                            @JsonProperty("type") String type) {
    this.businessEntityId = businessEntityId;
    this.name = name;
    this.type = type;
  }

  public UUID getBusinessEntityId() { return businessEntityId; }
  public String getName() { return name; }
  public String getType() { return type; }
}
