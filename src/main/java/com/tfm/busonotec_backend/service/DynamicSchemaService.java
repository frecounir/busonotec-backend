package com.tfm.busonotec_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collections;

@Service
public class DynamicSchemaService {
  private static final Logger log = LoggerFactory.getLogger(DynamicSchemaService.class);
  private final JdbcTemplate jdbc;
  private final Set<String> createdEntities = ConcurrentHashMap.newKeySet();

  public DynamicSchemaService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  /** Execute SQL statements mapped by entity name. Validates SQL before execution. */
  public void executeStatements(Map<String, String> statementsByEntity) {
    if (statementsByEntity == null || statementsByEntity.isEmpty()) return;
    for (Map.Entry<String, String> e : statementsByEntity.entrySet()) {
      String entity = Objects.requireNonNull(e.getKey(), "Entity name must not be null");
      String sql = Objects.requireNonNull(e.getValue(), "SQL must not be null for entity " + entity);
      validateSql(sql);
      log.info("Executing SQL for entity {}: {}", entity, sql);
      jdbc.execute(sql);
      createdEntities.add(entity.toLowerCase());
      log.info("Created/registered entity: {}", entity);
    }
  }

  public boolean entityExists(String entityName) {
    return createdEntities.contains(entityName.toLowerCase());
  }

  /** Validate SQL before execution. Only allow CREATE TABLE and INSERT INTO. */
  private void validateSql(String sql) {
    if (sql == null || sql.isBlank()) throw new IllegalArgumentException("SQL statement is empty");
    String s = sql.trim().toUpperCase(Locale.ROOT);
    // reject multi-statement payloads (allow trailing ; at end)
    if (s.contains(";") && s.indexOf(";") != s.length() - 1) {
      throw new IllegalArgumentException("Multiple statements or trailing content not allowed");
    }
    if (s.startsWith("CREATE TABLE") || s.startsWith("INSERT INTO")) {
      return;
    }
    if (s.contains("DROP ") || s.contains("TRUNCATE ") || s.contains("DELETE FROM") || s.contains("ALTER ")) {
      throw new IllegalArgumentException("Dangerous SQL keywords detected");
    }
    throw new IllegalArgumentException("Only CREATE TABLE and INSERT INTO statements are allowed");
  }

  // For testing/debug
  public Set<String> getCreatedEntities() { return Collections.unmodifiableSet(createdEntities); }
}
