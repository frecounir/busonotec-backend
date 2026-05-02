package com.tfm.busonotec_backend.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Domain-only validator: no framework or DB dependencies.
 * Contains pure validation logic for EntityModel, FieldModel and RelationshipModel.
 */
public class ModelValidator {
  private static final Logger log = LoggerFactory.getLogger(ModelValidator.class);
  private static final Pattern NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");
  private static final Set<String> ALLOWED_TYPES = Set.of("STRING", "INTEGER", "BOOLEAN", "TEXT", "DATE", "TIMESTAMP");
  private static final Set<String> ALLOWED_REL_TYPES = Set.of("ONE_TO_MANY", "MANY_TO_ONE", "ONE_TO_ONE", "MANY_TO_MANY");

  private void fail(String msg) {
    log.warn(msg);
    throw new IllegalArgumentException(msg);
  }

  /** Validate list of entities and relationships between them. */
  public void validateEntities(List<EntityModel> entities) {
    if (entities == null || entities.isEmpty()) {
      fail("No entities provided");
    }
    Set<String> names = new HashSet<>();
    for (EntityModel e : entities) {
      validateEntity(e);
      String n = e.getName().toLowerCase();
      if (!names.add(n)) {
        fail("Duplicate entity name: " + e.getName());
      }
    }
    // validate relationships after all entity names collected
    validateRelationships(entities);
  }

  /** Validate a single entity (fields and names). */
  public void validateEntity(EntityModel e) {
    if (e == null) fail("Entity is null");
    if (e.getName() == null || !NAME.matcher(e.getName()).matches()) {
      fail("Invalid entity name: " + e.getName());
    }
    if (e.getFields() == null || e.getFields().isEmpty()) {
      fail("Entity must have at least one field: " + e.getName());
    }
    Set<String> fieldNames = new HashSet<>();
    for (FieldModel f : e.getFields()) {
      if (f == null) fail("Null field in entity: " + e.getName());
      if (f.getName() == null || !NAME.matcher(f.getName()).matches()) {
        fail("Invalid field name: " + f.getName() + " in " + e.getName());
      }
      if (f.getType() == null || !ALLOWED_TYPES.contains(f.getType().toUpperCase(Locale.ROOT))) {
        fail("Unsupported field type: " + f.getType() + " in " + e.getName());
      }
      if (!fieldNames.add(f.getName().toLowerCase())) {
        fail("Duplicate field: " + f.getName() + " in " + e.getName());
      }
    }
  }

  /** Validate relationship models reference existing entities and use allowed types. */
  private void validateRelationships(List<EntityModel> entities) {
    // build set of entity names
    Set<String> names = new HashSet<>();
    for (EntityModel e : entities) names.add(e.getName().toLowerCase());

    for (EntityModel e : entities) {
      List<RelationshipModel> rels = e.getRelationships();
      if (rels == null) continue;
      for (RelationshipModel r : rels) {
        if (r == null) fail("Null relationship in entity: " + e.getName());
        if (r.getFromEntity() == null || r.getToEntity() == null) {
          fail("Relationship must have fromEntity and toEntity in " + e.getName());
        }
        String from = r.getFromEntity().toLowerCase();
        String to = r.getToEntity().toLowerCase();
        if (!names.contains(from)) {
          fail("Relationship references unknown fromEntity: " + r.getFromEntity());
        }
        if (!names.contains(to)) {
          fail("Relationship references unknown toEntity: " + r.getToEntity());
        }
        if (r.getType() == null || !ALLOWED_REL_TYPES.contains(r.getType().toUpperCase(Locale.ROOT))) {
          fail("Unsupported relationship type: " + r.getType() + " from " + r.getFromEntity() + " to " + r.getToEntity());
        }
        // avoid circular self-referencing definitions that are likely a mistake
        if (from.equals(to) && ("MANY_TO_MANY".equalsIgnoreCase(r.getType()) || "ONE_TO_ONE".equalsIgnoreCase(r.getType()))) {
          fail("Self-referential relationship of type " + r.getType() + " not allowed for entity " + e.getName());
        }
      }
    }
  }
}
