package com.tfm.busonotec_backend.util;

import java.util.regex.Pattern;

public final class IdentifierValidator {
  private static final Pattern IDENTIFIER = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");

  private IdentifierValidator() {}

  public static void requireValid(String value, String blankLabel, String invalidLabel) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(blankLabel + " must be provided");
    }
    if (!IDENTIFIER.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid " + invalidLabel + ": " + value);
    }
  }

  public static boolean isValid(String value) {
    return value != null && IDENTIFIER.matcher(value).matches();
  }
}
