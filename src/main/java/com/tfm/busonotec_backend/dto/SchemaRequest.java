package com.tfm.busonotec_backend.dto;

import com.tfm.busonotec_backend.domain.EntityModel;

import java.util.List;

public class SchemaRequest {
  private List<EntityModel> entities;

  public SchemaRequest() {}
  public SchemaRequest(List<EntityModel> entities) { this.entities = entities; }

  public List<EntityModel> getEntities() { return entities; }
  public void setEntities(List<EntityModel> entities) { this.entities = entities; }
}
