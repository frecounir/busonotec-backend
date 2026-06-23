package com.tfm.busonotec_backend.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum RelationshipType {
  MANY_TO_ONE("many_to_one"),
  ONE_TO_ONE("one_to_one");

  private final String value;

  RelationshipType(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static Optional<RelationshipType> from(String value) {
    String normalized = normalize(value);
    return Arrays.stream(values())
        .filter(type -> type.value.equals(normalized))
        .findFirst();
  }

  public static String normalize(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }
}
