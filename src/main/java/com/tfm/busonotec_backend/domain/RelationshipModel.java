package com.tfm.busonotec_backend.domain;

public class RelationshipModel {
  private String fromEntity;
  private String toEntity;
  private String type; // e.g., ONE_TO_MANY, MANY_TO_ONE, etc.

  public RelationshipModel() {}

  public RelationshipModel(String fromEntity, String toEntity, String type) {
    this.fromEntity = fromEntity;
    this.toEntity = toEntity;
    this.type = type;
  }

  public String getFromEntity() { return fromEntity; }
  public void setFromEntity(String fromEntity) { this.fromEntity = fromEntity; }

  public String getToEntity() { return toEntity; }
  public void setToEntity(String toEntity) { this.toEntity = toEntity; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
}
