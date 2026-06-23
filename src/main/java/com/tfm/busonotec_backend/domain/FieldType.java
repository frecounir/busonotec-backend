package com.tfm.busonotec_backend.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum FieldType {
  STRING("string"),
  NUMBER("number"),
  BOOLEAN("boolean"),
  DATE("date"),
  RELATIONSHIP("relationship");

  private final String value;

  FieldType(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public boolean is(String candidate) {
    return value.equals(normalize(candidate));
  }

  public static Optional<FieldType> from(String value) {
    String normalized = normalize(value);
    return Arrays.stream(values())
        .filter(type -> type.value.equals(normalized))
        .findFirst();
  }

  public static FieldType requireSupported(String value, String errorPrefix) {
    return from(value)
        .orElseThrow(() -> new IllegalArgumentException(errorPrefix + value));
  }

  public static String normalize(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }
}
