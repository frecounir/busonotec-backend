package com.tfm.busonotec_backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EntityModel {
  private String name;
  private List<FieldModel> fields;
  private List<RelationshipModel> relationships;

  @JsonCreator
  public EntityModel(@JsonProperty("name") String name,
                     @JsonProperty("fields") List<FieldModel> fields,
                     @JsonProperty("relationships") List<RelationshipModel> relationships) {
    this.name = name;
    this.fields = fields;
    this.relationships = relationships;
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public List<FieldModel> getFields() { return fields; }
  public void setFields(List<FieldModel> fields) { this.fields = fields; }

  public List<RelationshipModel> getRelationships() { return relationships; }
  public void setRelationships(List<RelationshipModel> relationships) { this.relationships = relationships; }
}
