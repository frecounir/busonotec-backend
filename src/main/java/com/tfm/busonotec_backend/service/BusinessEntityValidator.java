package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.util.IdentifierValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BusinessEntityValidator {
  private static final Logger log = LoggerFactory.getLogger(BusinessEntityValidator.class);

  public void validateForCreate(BusinessEntityRequest request) {
    String name = request == null ? null : request.getName();
    try {
      IdentifierValidator.requireValid(name, "Entity name", "entity name");
    } catch (IllegalArgumentException ex) {
      logValidationFailure(name);
      throw ex;
    }
  }

  private void logValidationFailure(String name) {
    if (name == null || name.isBlank()) {
      log.warn("Validation failed: entity name blank");
      return;
    }
    log.warn("Validation failed: invalid entity name {}", name);
  }
}
