package com.tfm.busonotec_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for creating a BusinessEntity.
 */
public class BusinessEntityRequest {
  private final String name;
  private final String description;

  @JsonCreator
  public BusinessEntityRequest(@JsonProperty("name") String name,
                               @JsonProperty("description") String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() { return name; }
  public String getDescription() { return description; }
}
