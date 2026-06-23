package com.tfm.busonotec_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * DTO for creating a BusinessEntity.
 */
@Getter
@Schema(description = "Payload used to create a logical business entity and its physical table.")
public class BusinessEntityRequest {
  @Schema(
      description = "Unique business entity name. It must start with a letter and can contain letters, numbers, and underscores.",
      example = "Students",
      minLength = 1,
      maxLength = 63,
      pattern = "^[a-zA-Z][a-zA-Z0-9_]{0,62}$",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private final String name;

  @Schema(
      description = "Human-readable description of the business entity.",
      example = "Students enrolled in academic activities"
  )
  private final String description;

  @JsonCreator
  public BusinessEntityRequest(@JsonProperty("name") String name,
                               @JsonProperty("description") String description) {
    this.name = name;
    this.description = description;
  }
}
