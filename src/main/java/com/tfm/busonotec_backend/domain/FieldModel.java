package com.tfm.busonotec_backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldModel {
  private String name;
  private String type; // logical type: STRING, INTEGER, BOOLEAN, TEXT, DATE, TIMESTAMP

  @JsonCreator
  public FieldModel(@JsonProperty("name") String name,
                    @JsonProperty("type") String type) {
    this.name = name;
    this.type = type;
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
}
