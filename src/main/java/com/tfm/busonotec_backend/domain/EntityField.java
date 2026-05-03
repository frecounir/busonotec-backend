package com.tfm.busonotec_backend.domain;

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

  public EntityField() {}

  public EntityField(UUID id, String name, String type, UUID businessEntityId, FieldDetail detail) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.businessEntityId = businessEntityId;
    this.detail = detail;
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
}
