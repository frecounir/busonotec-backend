package com.tfm.busonotec_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
 Service responsible for CRUD over dynamic entities.
 Keeps DB access and validation inside service layer.
 */
@Service
public class GenericDataService {
  private static final Logger log = LoggerFactory.getLogger(GenericDataService.class);
  private final JdbcTemplate jdbc;
  private final DynamicSchemaService schemaService;

  public GenericDataService(JdbcTemplate jdbc, DynamicSchemaService schemaService) {
    this.jdbc = jdbc;
    this.schemaService = schemaService;
  }

  public List<Map<String, Object>> listData(String entity) {
    validateEntityName(entity);
    if (!schemaService.entityExists(entity)) {
      throw new IllegalArgumentException("Unknown entity: " + entity);
    }
    String table = "\"" + entity.toLowerCase() + "\"";
    log.info("Listing up to 100 rows from {}", table);
    return jdbc.queryForList("SELECT * FROM " + table + " LIMIT 100");
  }

  public void insertRow(String entity, Map<String, Object> payload) {
    validateEntityName(entity);
    if (!schemaService.entityExists(entity)) {
      throw new IllegalArgumentException("Unknown entity: " + entity);
    }
    if (payload == null || payload.isEmpty()) throw new IllegalArgumentException("Payload empty");
    for (String k : payload.keySet()) {
      if (!k.matches("^[a-zA-Z][a-zA-Z0-9_]{0,62}$")) {
        throw new IllegalArgumentException("Invalid column name: " + k);
      }
    }
    List<String> keys = new ArrayList<>(payload.keySet());
    String cols = keys.stream().map(s -> "\"" + s.toLowerCase() + "\"").collect(Collectors.joining(", "));
    String placeholders = String.join(", ", Collections.nCopies(keys.size(), "?"));
    Object[] values = keys.stream().map(payload::get).toArray();
    String table = "\"" + entity.toLowerCase() + "\"";
    String sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
    log.info("Inserting into {} columns={} payloadKeys={}", table, cols, keys);
    jdbc.update(sql, values);
  }

  private void validateEntityName(String entity) {
    if (entity == null || entity.isBlank()) {
      log.warn("Validation failed: entity name is blank");
      throw new IllegalArgumentException("Entity name must be provided");
    }
    if (!entity.matches("^[a-zA-Z][a-zA-Z0-9_]{0,62}$")) {
      log.warn("Validation failed: invalid entity name {}", entity);
      throw new IllegalArgumentException("Invalid entity name: " + entity);
    }
  }
}
