package com.tfm.busonotec_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Additional metadata for an EntityField.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldDetail {
  private Boolean nullable;
  private String constraints; // simple textual representation for now
  private String defaultValue;
}
