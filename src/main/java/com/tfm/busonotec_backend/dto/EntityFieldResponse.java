package com.tfm.busonotec_backend.dto;

import java.util.UUID;

/**
 * DTO returned after creating or listing fields.
 */
public class EntityFieldResponse {
  private UUID id;
  private UUID businessEntityId;
  private String name;
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
