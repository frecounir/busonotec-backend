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
import java.util.regex.Pattern;

@Service
public class DynamicSchemaService {
  private static final Logger log = LoggerFactory.getLogger(DynamicSchemaService.class);
  private static final Pattern NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");
  private final JdbcTemplate jdbc;
  private final Set<String> createdEntities = ConcurrentHashMap.newKeySet();

  public DynamicSchemaService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  /** Execute schema creation statements mapped by entity name. Validates SQL before execution. */
  public void executeStatements(Map<String, String> statementsByEntity) {
    if (statementsByEntity == null || statementsByEntity.isEmpty()) return;
    for (Map.Entry<String, String> e : statementsByEntity.entrySet()) {
      String entity = Objects.requireNonNull(e.getKey(), "Entity name must not be null");
      String sql = Objects.requireNonNull(e.getValue(), "SQL must not be null for entity " + entity);
      validateIdentifier(entity, "entity name");
      validateCreateTableSql(entity, sql);
      log.info("Executing SQL for entity {}: {}", entity, sql);
      jdbc.execute(sql);
      createdEntities.add(entity.toLowerCase(Locale.ROOT));
      log.info("Created/registered entity: {}", entity);
    }
  }

  public boolean entityExists(String entityName) {
    if (entityName == null || !NAME.matcher(entityName).matches()) {
      return false;
    }
    if (createdEntities.contains(entityName.toLowerCase())) {
      return true;
    }
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name = ?",
        Integer.class,
        entityName.toLowerCase(Locale.ROOT)
    );
    return count != null && count > 0;
  }

  public void addColumn(String entityName, String fieldName, String logicalType) {
    validateIdentifier(entityName, "entity name");
    validateIdentifier(fieldName, "field name");
    if (!entityExists(entityName)) {
      throw new IllegalArgumentException("Physical table does not exist for entity: " + entityName);
    }
    String sql = "ALTER TABLE " + quote(entityName)
        + " ADD COLUMN IF NOT EXISTS " + quote(fieldName)
        + " " + mapColumnType(logicalType);
    log.info("Adding column {}.{} with SQL: {}", entityName, fieldName, sql);
    jdbc.execute(sql);
  }

  public void dropColumn(String entityName, String fieldName) {
    validateIdentifier(entityName, "entity name");
    validateIdentifier(fieldName, "field name");
    if ("id".equalsIgnoreCase(fieldName)) {
      throw new IllegalArgumentException("Field name 'id' is reserved");
    }
    if (!entityExists(entityName)) {
      throw new IllegalArgumentException("Physical table does not exist for entity: " + entityName);
    }
    String sql = "ALTER TABLE " + quote(entityName)
        + " DROP COLUMN IF EXISTS " + quote(fieldName);
    log.info("Dropping column {}.{} with SQL: {}", entityName, fieldName, sql);
    jdbc.execute(sql);
  }

  public void dropEntityTable(String entityName) {
    validateIdentifier(entityName, "entity name");
    String sql = "DROP TABLE IF EXISTS " + quote(entityName);
    log.info("Dropping physical table for entity {} with SQL: {}", entityName, sql);
    jdbc.execute(sql);
    createdEntities.remove(entityName.toLowerCase(Locale.ROOT));
  }

  /** Validate SQL before execution. Only allow the expected CREATE TABLE statement. */
  private void validateCreateTableSql(String entityName, String sql) {
    if (sql == null || sql.isBlank()) throw new IllegalArgumentException("SQL statement is empty");
    String s = sql.trim();
    // reject multi-statement payloads (allow trailing ; at end)
    if (s.contains(";") && s.indexOf(";") != s.length() - 1) {
      throw new IllegalArgumentException("Multiple statements or trailing content not allowed");
    }
    if (s.endsWith(";")) {
      s = s.substring(0, s.length() - 1).trim();
    }
    String upper = s.toUpperCase(Locale.ROOT);
    if (upper.contains("DROP ") || upper.contains("TRUNCATE ") || upper.contains("DELETE FROM")
        || upper.contains("ALTER ") || upper.contains("INSERT INTO")) {
      throw new IllegalArgumentException("Dangerous SQL keywords detected");
    }
    String expected = "CREATE TABLE IF NOT EXISTS " + quote(entityName) + " (id UUID PRIMARY KEY)";
    if (!s.equals(expected)) {
      throw new IllegalArgumentException("Only the expected CREATE TABLE statement is allowed");
    }
  }

  // For testing/debug
  public Set<String> getCreatedEntities() { return Collections.unmodifiableSet(createdEntities); }

  private void validateIdentifier(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must be provided");
    }
    if (!NAME.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid " + label + ": " + value);
    }
  }

  private String quote(String identifier) {
    return "\"" + identifier.toLowerCase(Locale.ROOT) + "\"";
  }

  private String mapColumnType(String logicalType) {
    if (logicalType == null || logicalType.isBlank()) {
      throw new IllegalArgumentException("Field type must be provided");
    }
    return switch (logicalType.toLowerCase(Locale.ROOT)) {
      case "string" -> "VARCHAR(255)";
      case "number" -> "NUMERIC";
      case "boolean" -> "BOOLEAN";
      case "date" -> "DATE";
      default -> throw new IllegalArgumentException("Unsupported field type: " + logicalType);
    };
  }
}
