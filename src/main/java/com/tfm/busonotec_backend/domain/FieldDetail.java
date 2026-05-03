package com.tfm.busonotec_backend.domain;

/**
 * Additional metadata for an EntityField.
 */
public class FieldDetail {
  private Boolean nullable;
  private String constraints; // simple textual representation for now
  private String defaultValue;

  public FieldDetail() {}

  public FieldDetail(Boolean nullable, String constraints, String defaultValue) {
    this.nullable = nullable;
    this.constraints = constraints;
    this.defaultValue = defaultValue;
  }

  public Boolean getNullable() { return nullable; }
  public void setNullable(Boolean nullable) { this.nullable = nullable; }

  public String getConstraints() { return constraints; }
  public void setConstraints(String constraints) { this.constraints = constraints; }

  public String getDefaultValue() { return defaultValue; }
  public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
}
