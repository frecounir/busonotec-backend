package com.tfm.busonotec_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Core domain: BusinessEntity represents a logical entity in the system.
 * It is framework-agnostic and keeps only domain concerns.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessEntity {
  private UUID id;
  private String name;
  private String description;
}
