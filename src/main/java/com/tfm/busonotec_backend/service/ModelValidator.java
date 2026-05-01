package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.EntityModel;
import com.tfm.busonotec_backend.domain.FieldModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ModelValidator {
  private static final Pattern NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");
  private static final Set<String> ALLOWED_TYPES = Set.of("STRING", "INTEGER", "BOOLEAN", "TEXT", "DATE", "TIMESTAMP");

  public void validateEntities(List<EntityModel> entities) {
    if (entities == null || entities.isEmpty()) throw new IllegalArgumentException("No entities provided");
    Set<String> names = new HashSet<>();
    for (EntityModel e : entities) {
      validateEntity(e);
      if (!names.add(e.getName().toLowerCase())) {
        throw new IllegalArgumentException("Duplicate entity name: " + e.getName());
      }
    }
  }

  public void validateEntity(EntityModel e) {
    if (e.getName() == null || !NAME.matcher(e.getName()).matches()) {
      throw new IllegalArgumentException("Invalid entity name: " + e.getName());
    }
    if (e.getFields() == null || e.getFields().isEmpty()) {
      throw new IllegalArgumentException("Entity must have at least one field: " + e.getName());
    }
    Set<String> fieldNames = new HashSet<>();
    for (FieldModel f : e.getFields()) {
      if (f.getName() == null || !NAME.matcher(f.getName()).matches()) {
        throw new IllegalArgumentException("Invalid field name: " + f.getName() + " in " + e.getName());
      }
      if (!ALLOWED_TYPES.contains(f.getType())) {
        throw new IllegalArgumentException("Unsupported field type: " + f.getType() + " in " + e.getName());
      }
      if (!fieldNames.add(f.getName().toLowerCase())) {
        throw new IllegalArgumentException("Duplicate field: " + f.getName() + " in " + e.getName());
      }
    }
  }
}
