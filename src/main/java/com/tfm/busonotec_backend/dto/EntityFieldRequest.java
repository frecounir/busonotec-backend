package com.tfm.busonotec_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * DTO to create an EntityField.
 */
public class EntityFieldRequest {
  private final UUID businessEntityId;
  private final String name;
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
