package com.tfm.busonotec_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 Execute DDL safely. Keeps a registry of created entities.
 */
@Service
public class DynamicSchemaService {
  private static final Logger log = LoggerFactory.getLogger(DynamicSchemaService.class);
  private final JdbcTemplate jdbc;
  private final Set<String> createdEntities = ConcurrentHashMap.newKeySet();

  public DynamicSchemaService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  /**
   * Execute SQL statements mapped by entity name. Table/entity names must be provided explicitly
   * to avoid any fragile SQL parsing.
   */
  public void executeStatements(Map<String, String> statementsByEntity) {
    if (statementsByEntity == null || statementsByEntity.isEmpty()) return;
    for (Map.Entry<String, String> e : statementsByEntity.entrySet()) {
      String entity = Objects.requireNonNull(e.getKey(), "Entity name must not be null");
      String sql = Objects.requireNonNull(e.getValue(), "SQL must not be null for entity " + entity);
      // new private validator wrapper
      validateSql(sql);
      log.info("Executing SQL for entity {}: {}", entity, sql);
      jdbc.execute(sql);
      createdEntities.add(entity.toLowerCase());
      log.info("Created/registered entity: {}", entity);
    }
  }

  /**
   * Maintain backward compatibility: avoid calling this as it relied on parsing SQL.
   */
  @Deprecated
  public void executeStatements(List<String> sqlStatements) {
    throw new UnsupportedOperationException("Use executeStatements(Map<String,String>) with explicit entity names");
  }

  // private wrapper to satisfy naming requirement while delegating to the public validator
  private void validateSql(String sql) {
    try {
      validateSqlStatement(sql);
    } catch (IllegalArgumentException ex) {
      log.warn("SQL validation failed: {}", ex.getMessage());
      throw ex;
    }
  }

  public boolean entityExists(String entityName) {
    return createdEntities.contains(entityName.toLowerCase());
  }

  /**
   * Validate SQL content before executing. Only allow CREATE TABLE and INSERT statements.
   * Throws IllegalArgumentException for unsafe or unsupported SQL.
   */
  public void validateSqlStatement(String sql) {
    if (sql == null || sql.isBlank()) throw new IllegalArgumentException("SQL statement is empty");
    String s = sql.trim().toUpperCase(Locale.ROOT);
    // Only allow single statements
    if (s.contains(";") && s.indexOf(";") != s.length() - 1) {
      throw new IllegalArgumentException("Multiple statements or trailing content not allowed");
    }
    if (s.startsWith("CREATE TABLE")) return;
    if (s.startsWith("INSERT INTO")) return;
    // explicitly disallow dangerous keywords
    if (s.contains("DROP ") || s.contains("TRUNCATE ") || s.contains("DELETE FROM") || s.contains("ALTER ")) {
      throw new IllegalArgumentException("Dangerous SQL keywords detected");
    }
    throw new IllegalArgumentException("Only CREATE TABLE and INSERT statements are allowed");
  }
}
