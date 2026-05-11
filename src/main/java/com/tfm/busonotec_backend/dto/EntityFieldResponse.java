package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO returned after creating or listing fields.
 */
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
      allowableValues = {"string", "number", "boolean", "date"}
  )
  private String type;

  public EntityFieldResponse() {}

  public EntityFieldResponse(UUID id, UUID businessEntityId, String name, String type) {
    this.id = id;
    this.businessEntityId = businessEntityId;
    this.name = name;
    this.type = type;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getBusinessEntityId() { return businessEntityId; }
  public void setBusinessEntityId(UUID businessEntityId) { this.businessEntityId = businessEntityId; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
}
